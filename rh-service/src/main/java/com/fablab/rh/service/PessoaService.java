package com.fablab.rh.service;

import com.fablab.rh.dto.FacetaResponse;
import com.fablab.rh.dto.FiltrosPessoasResponse;
import com.fablab.rh.dto.PaginacaoResponse;
import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.dto.PessoasPaginaResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.PessoaMapper;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.PessoaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão das pessoas do Fab Lab (cadastro, consulta e atualização).
 */
@Service
public class PessoaService {

    private static final int PAGE_SIZE_MAX = 100;

    private final PessoaRepository pessoaRepository;
    private final FuncionarioRepository funcionarioRepository;

    public PessoaService(PessoaRepository pessoaRepository,
                         FuncionarioRepository funcionarioRepository) {
        this.pessoaRepository = pessoaRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional
    public PessoaResponse cadastrar(PessoaRequest request) {
        validarMatriculaDisponivel(request.matricula(), null);
        Pessoa pessoa = PessoaMapper.toEntity(request);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
    }

    /**
     * Lista paginada com busca e filtros. Recrutando não acessa o módulo;
     * Estagiário enxerga apenas o próprio registro; demais níveis veem todos.
     */
    @Transactional(readOnly = true)
    public PessoasPaginaResponse listar(String busca, String departamento, NivelAcesso nivel,
                                        PessoaStatus status, int page, int pageSize,
                                        RhPrincipal principal) {
        if (principal != null && principal.nivel() == NivelAcesso.RECRUTANDO) {
            throw new ForbiddenException("Acesso negado ao módulo de pessoas");
        }
        String buscaNormalizada = busca == null || busca.isBlank() ? null : busca.trim();
        String departamentoNormalizado =
                departamento == null || departamento.isBlank() ? null : departamento.trim();
        int pagina = Math.max(page, 1);
        int tamanho = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);

        if (principal != null && !principal.isAdmin() && principal.nivel() == NivelAcesso.ESTAGIARIO) {
            return paginaDoProprioRegistro(buscaNormalizada, departamentoNormalizado, nivel, status,
                    pagina, tamanho, principal);
        }

        Page<Pessoa> resultado = pessoaRepository.buscarComFiltros(
                buscaNormalizada, status, nivel, departamentoNormalizado,
                PageRequest.of(pagina - 1, tamanho));
        List<PessoaResponse> pessoas = resultado.getContent().stream()
                .map(PessoaMapper::toResponse)
                .toList();
        return new PessoasPaginaResponse(
                pessoas,
                new PaginacaoResponse(pagina, tamanho, resultado.getTotalElements(),
                        resultado.getTotalPages()),
                facetas(buscaNormalizada));
    }

    /** Página contendo apenas o próprio registro do estagiário (quando casa com os filtros). */
    private PessoasPaginaResponse paginaDoProprioRegistro(String busca, String departamento,
                                                          NivelAcesso nivel, PessoaStatus status,
                                                          int pagina, int tamanho,
                                                          RhPrincipal principal) {
        Pessoa propria = principal.idPessoa() == null ? null
                : pessoaRepository.findById(principal.idPessoa()).orElse(null);
        List<PessoaResponse> pessoas = new ArrayList<>();
        List<FacetaResponse> statuses = new ArrayList<>();
        List<FacetaResponse> niveis = new ArrayList<>();
        List<FacetaResponse> setores = new ArrayList<>();
        if (propria != null && casaComFiltros(propria, busca, departamento, nivel, status, principal)) {
            pessoas.add(PessoaMapper.toResponse(propria));
            statuses.add(faceta(propria.getStatus().name().toLowerCase(), rotuloStatus(propria.getStatus()), 1));
            funcionarioRepository.findByPessoaId(propria.getId()).ifPresent(func -> {
                niveis.add(faceta(func.getNivelAcesso().name().toLowerCase(),
                        func.getNivelAcesso().getLabel(), 1));
                if (func.getDepartamento() != null) {
                    setores.add(faceta(func.getDepartamento(), func.getDepartamento(), 1));
                }
            });
        }
        int totalPages = pessoas.isEmpty() ? 0 : 1;
        List<PessoaResponse> paginaItens = pagina == 1 ? pessoas : List.of();
        return new PessoasPaginaResponse(paginaItens,
                new PaginacaoResponse(pagina, tamanho, pessoas.size(), totalPages),
                new FiltrosPessoasResponse(statuses, niveis, setores));
    }

    private boolean casaComFiltros(Pessoa pessoa, String busca, String departamento,
                                   NivelAcesso nivel, PessoaStatus status, RhPrincipal principal) {
        if (status != null && pessoa.getStatus() != status) {
            return false;
        }
        if (busca != null) {
            String termo = busca.toLowerCase();
            boolean casa = pessoa.getNomeCompleto().toLowerCase().contains(termo)
                    || pessoa.getMatricula().toLowerCase().contains(termo)
                    || (pessoa.getContato() != null && pessoa.getContato().toLowerCase().contains(termo));
            if (!casa) {
                return false;
            }
        }
        if (nivel != null || departamento != null) {
            Funcionario func = funcionarioRepository.findByPessoaId(pessoa.getId()).orElse(null);
            if (func == null) {
                return false;
            }
            if (nivel != null && func.getNivelAcesso() != nivel) {
                return false;
            }
            if (departamento != null && (func.getDepartamento() == null
                    || !func.getDepartamento().equalsIgnoreCase(departamento))) {
                return false;
            }
        }
        return true;
    }

    private FiltrosPessoasResponse facetas(String busca) {
        List<FacetaResponse> statuses = new ArrayList<>();
        for (Object[] linha : pessoaRepository.contarPorStatus(busca)) {
            PessoaStatus status = (PessoaStatus) linha[0];
            statuses.add(faceta(status.name().toLowerCase(), rotuloStatus(status),
                    (Long) linha[1]));
        }
        List<FacetaResponse> niveis = new ArrayList<>();
        for (Object[] linha : funcionarioRepository.contarPorNivel()) {
            NivelAcesso nivel = (NivelAcesso) linha[0];
            niveis.add(faceta(nivel.name().toLowerCase(), nivel.getLabel(), (Long) linha[1]));
        }
        List<FacetaResponse> setores = new ArrayList<>();
        for (Object[] linha : funcionarioRepository.contarPorDepartamento()) {
            String departamento = (String) linha[0];
            setores.add(faceta(departamento, departamento, (Long) linha[1]));
        }
        return new FiltrosPessoasResponse(statuses, niveis, setores);
    }

    private FacetaResponse faceta(String id, String label, long count) {
        return new FacetaResponse(id, label, count);
    }

    private String rotuloStatus(PessoaStatus status) {
        return switch (status) {
            case ATIVO -> "Ativo";
            case INATIVO -> "Inativo";
            case RECRUTANDO -> "Recrutando";
        };
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscar(Long id, RhPrincipal principal) {
        Pessoa pessoa = obter(id);
        validarAcesso(pessoa, principal);
        return PessoaMapper.toResponse(pessoa);
    }

    @Transactional
    public PessoaResponse atualizar(Long id, PessoaRequest request, RhPrincipal principal) {
        Pessoa pessoa = obter(id);
        validarAcesso(pessoa, principal);
        validarMatriculaDisponivel(request.matricula(), id);
        PessoaMapper.update(pessoa, request);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
    }

    public Pessoa obter(Long id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + id));
    }

    private void validarMatriculaDisponivel(String matricula, Long idAtual) {
        if (pessoaRepository.existsByMatricula(matricula)) {
            Pessoa existente = pessoaRepository.findByMatricula(matricula).orElse(null);
            if (existente == null || !existente.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Matrícula já cadastrada: " + matricula);
            }
        }
    }

    /**
     * Admin, Bolsista e Voluntário veem todos os registros; Estagiário,
     * Recrutando ou principal sem nível veem apenas o próprio registro.
     */
    private void validarAcesso(Pessoa pessoa, RhPrincipal principal) {
        if (principal == null || principal.isAdmin()
                || principal.nivel() == NivelAcesso.BOLSISTA
                || principal.nivel() == NivelAcesso.VOLUNTARIO) {
            return;
        }
        if (principal.idPessoa() == null || !principal.idPessoa().equals(pessoa.getId())) {
            throw new ForbiddenException("Acesso apenas aos seus próprios dados");
        }
    }
}