package com.fablab.auth.service;

import com.fablab.auth.dto.Usuarios;
import com.fablab.auth.entity.Login;
import com.fablab.auth.entity.Role;
import com.fablab.auth.entity.SituacaoUsuario;
import com.fablab.auth.entity.UserPermission;
import com.fablab.auth.exception.ResourceNotFoundException;
import com.fablab.auth.repository.LoginRepository;
import com.fablab.auth.repository.UserPermissionRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de contas de acesso (nível/situação) sobre {@code login} +
 * {@code user_permissions}.
 *
 * <p>Não duplica o RH: identidade física é do {@code rh-service}; aqui
 * operam-se apenas conta, nível RBAC e situação.</p>
 */
@Service
public class UsuarioService {

    private final LoginRepository loginRepository;
    private final UserPermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(LoginRepository loginRepository,
                          UserPermissionRepository permissionRepository,
                          PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lista contas com filtros, paginação e contagens servidas pelo backend
     * (C-6: o front nunca deriva).
     */
    @Transactional(readOnly = true)
    public Usuarios.PaginaUsuariosResponse listar(String search, List<Integer> niveis,
                                                  List<String> situacoes, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Set<Integer> niveisFiltro = niveis == null ? Set.of() : new HashSet<>(niveis);
        for (Integer nivel : niveisFiltro) {
            try {
                Role.fromCode(nivel);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Nível inválido no filtro: informe valores de 0 a 4");
            }
        }
        Set<SituacaoUsuario> situacoesFiltro = new HashSet<>();
        if (situacoes != null) {
            for (String raw : situacoes) {
                try {
                    situacoesFiltro.add(SituacaoUsuario.parse(raw));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("Situação inválida no filtro: informe Ativo, Pendente ou Desativado");
                }
            }
        }

        List<Login> todas = loginRepository.findAll();
        String termo = search == null ? "" : search.trim().toLowerCase();

        List<Login> filtradas = new ArrayList<>();
        for (Login login : todas) {
            if (!termo.isEmpty()
                    && !login.getEmail().toLowerCase().contains(termo)
                    && !login.getNomeUsuario().toLowerCase().contains(termo)) {
                continue;
            }
            if (!situacoesFiltro.isEmpty() && !situacoesFiltro.contains(login.getSituacao())) {
                continue;
            }
            Role role = roleOf(login);
            if (!niveisFiltro.isEmpty() && (role == null || !niveisFiltro.contains(role.getCode()))) {
                continue;
            }
            filtradas.add(login);
        }
        filtradas.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        long total = filtradas.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int from = Math.min(safePage * safeSize, filtradas.size());
        int to = Math.min(from + safeSize, filtradas.size());
        List<Usuarios.UsuarioResponse> content = filtradas.subList(from, to).stream().map(this::toResponse).toList();

        long ativos = filtradas.stream().filter(l -> l.getSituacao() == SituacaoUsuario.ATIVO).count();
        long pendentes = filtradas.stream().filter(l -> l.getSituacao() == SituacaoUsuario.PENDENTE).count();
        long desativados = filtradas.stream().filter(l -> l.getSituacao() == SituacaoUsuario.DESATIVADO).count();
        Map<String, Long> porNivel = new java.util.LinkedHashMap<>();
        for (Role role : Role.values()) {
            long count = filtradas.stream().filter(l -> role.equals(roleOf(l))).count();
            porNivel.put(role.name(), count);
        }

        return new Usuarios.PaginaUsuariosResponse(content, safePage, safeSize, total, totalPages,
                new Usuarios.Contagens(total, ativos, pendentes, desativados, porNivel));
    }

    /**
     * Cria conta + permissão (convite). Situação ausente = Pendente; nível
     * ausente = Recrutando (4).
     */
    @Transactional
    public Usuarios.UsuarioResponse criar(Usuarios.CriarUsuarioRequest request) {
        String email = request.email().trim();
        String nomeUsuario = request.nomeUsuario().trim();
        if (loginRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        if (loginRepository.existsByNomeUsuario(nomeUsuario)) {
            throw new IllegalArgumentException("Nome de usuário já cadastrado");
        }
        Role role = request.nivel() == null ? Role.RECRUTANDO : parseNivel(request.nivel());
        SituacaoUsuario situacao = request.situacao() == null || request.situacao().isBlank()
                ? SituacaoUsuario.PENDENTE
                : SituacaoUsuario.parse(request.situacao());

        Login login = new Login();
        login.setIdUser(request.idUser());
        login.setUuid(request.uuid() == null || request.uuid().isBlank()
                ? UUID.randomUUID().toString()
                : request.uuid().trim());
        login.setEmail(email);
        login.setNomeUsuario(nomeUsuario);
        login.setSenhaHash(passwordEncoder.encode(request.senha()));
        login.setSetor(request.setor());
        login.setSituacao(situacao);
        login = loginRepository.save(login);

        UserPermission permission = new UserPermission();
        permission.setIdUser(login.getIdUser());
        permission.setRole(role);
        permission.setActive(true);
        permissionRepository.save(permission);

        return toResponse(login);
    }

    /**
     * Atualiza conta (e-mail, nome de usuário, setor, nível, situação).
     */
    @Transactional
    public Usuarios.UsuarioResponse atualizar(Long id, Usuarios.AtualizarUsuarioRequest request) {
        Login login = loginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim();
            if (!email.equalsIgnoreCase(login.getEmail()) && loginRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("E-mail já cadastrado");
            }
            login.setEmail(email);
        }
        if (request.nomeUsuario() != null && !request.nomeUsuario().isBlank()) {
            String nome = request.nomeUsuario().trim();
            if (!nome.equalsIgnoreCase(login.getNomeUsuario()) && loginRepository.existsByNomeUsuario(nome)) {
                throw new IllegalArgumentException("Nome de usuário já cadastrado");
            }
            login.setNomeUsuario(nome);
        }
        if (request.setor() != null) {
            login.setSetor(request.setor());
        }
        if (request.situacao() != null && !request.situacao().isBlank()) {
            login.setSituacao(SituacaoUsuario.parse(request.situacao()));
        }
        login = loginRepository.save(login);

        if (request.nivel() != null) {
            Role role = parseNivel(request.nivel());
            upsertRole(login.getIdUser(), role);
        }
        return toResponse(login);
    }

    /**
     * Ativa/pende/desativa conta.
     */
    @Transactional
    public Usuarios.UsuarioResponse alterarStatus(Long id, String situacaoRaw) {
        Login login = loginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        login.setSituacao(SituacaoUsuario.parse(situacaoRaw));
        login = loginRepository.save(login);
        return toResponse(login);
    }

    private Role parseNivel(Integer codigo) {
        try {
            return Role.fromCode(codigo);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nível inválido: informe valores de 0 a 4");
        }
    }

    private Role roleOf(Login login) {
        return permissionRepository.findFirstByIdUserAndActiveTrue(login.getIdUser())
                .map(UserPermission::getRole)
                .orElse(null);
    }

    private void upsertRole(Long idUser, Role role) {
        UserPermission permission = permissionRepository.findFirstByIdUserAndActiveTrue(idUser).orElse(null);
        if (permission == null) {
            UserPermission created = new UserPermission();
            created.setIdUser(idUser);
            created.setRole(role);
            created.setActive(true);
            permissionRepository.save(created);
            return;
        }
        permission.setRole(role);
        permissionRepository.save(permission);
    }

    private Usuarios.UsuarioResponse toResponse(Login login) {
        Role role = roleOf(login);
        return new Usuarios.UsuarioResponse(
                login.getId(),
                login.getIdUser(),
                login.getEmail(),
                login.getNomeUsuario(),
                login.getSetor(),
                role == null ? null : role.name(),
                role == null ? null : role.getCode(),
                login.getSituacao() == null ? null : login.getSituacao().getLabel());
    }
}
