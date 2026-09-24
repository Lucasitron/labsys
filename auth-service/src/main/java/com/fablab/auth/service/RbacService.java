package com.fablab.auth.service;

import com.fablab.auth.dto.Permissoes;
import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.entity.Modulo;
import com.fablab.auth.entity.PermissaoNivel;
import com.fablab.auth.entity.Role;
import com.fablab.auth.exception.PermissaoInvalidaException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Matriz de permissões RBAC do sistema.
 */
@Service
public class RbacService {

    /**
     * Matriz de permissões por papel. O admin possui permissão total ("*").
     */
    private static final Map<Role, List<String>> MATRIX = Map.of(
            Role.ADMIN, List.of("*"),
            Role.BOLSISTA, List.of("catalogo:read", "projetos:write", "equipamentos:reserve",
                    "ponto:write", "relatorios:export"),
            Role.VOLUNTARIO, List.of("catalogo:read", "equipamentos:read"),
            Role.ESTAGIARIO, List.of("catalogo:read", "equipamentos:read", "ponto:write"),
            Role.RECRUTANDO, List.of("catalogo:read")
    );

    /**
     * Retorna a matriz de permissões de um papel.
     */
    public RbacResponse getMatrix(Role role) {
        return new RbacResponse(
                role.name(),
                role.getCode(),
                role.getLabel(),
                MATRIX.getOrDefault(role, List.of()));
    }

    /**
     * Células editáveis (módulo × nível) da tela Permissões. Padrão: Admin
     * edita tudo; demais níveis veem; Recrutando sem acesso.
     */
    private final Map<Modulo, Map<Role, PermissaoNivel>> cells = new ConcurrentHashMap<>();

    public RbacService() {
        for (Modulo modulo : Modulo.values()) {
            Map<Role, PermissaoNivel> column = new EnumMap<>(Role.class);
            column.put(Role.ADMIN, PermissaoNivel.EDITAR);
            column.put(Role.BOLSISTA, PermissaoNivel.VER);
            column.put(Role.VOLUNTARIO, PermissaoNivel.VER);
            column.put(Role.ESTAGIARIO, PermissaoNivel.VER);
            column.put(Role.RECRUTANDO, PermissaoNivel.NENHUM);
            cells.put(modulo, column);
        }
    }

    /**
     * Matriz completa: papéis (reaproveitada, sem recalcular) + células
     * (módulo × nível) + enums válidos.
     */
    public Permissoes.MatrizPermissoesResponse getFullMatrix() {
        List<RbacResponse> roles = Arrays.stream(Role.values()).map(this::getMatrix).toList();
        List<Permissoes.CelulaPermissao> matriz = new ArrayList<>();
        for (Modulo modulo : Modulo.values()) {
            for (Role role : Role.values()) {
                PermissaoNivel valor = cells.get(modulo).get(role);
                matriz.add(new Permissoes.CelulaPermissao(
                        modulo.getCode(), role.name(), role.getCode(), valor.getLabel()));
            }
        }
        Map<String, List<String>> enums = Map.of(
                "modulos", Arrays.stream(Modulo.values()).map(Modulo::getCode).toList(),
                "niveis", Arrays.stream(Role.values()).map(Role::name).toList(),
                "valores", Arrays.stream(PermissaoNivel.values()).map(PermissaoNivel::getLabel).toList());
        return new Permissoes.MatrizPermissoesResponse(roles, matriz, enums);
    }

    /**
     * Atualiza uma célula (módulo × nível), validando módulo/nível/valor
     * contra os enums (C-3: 422 PT se inválido).
     */
    public Permissoes.CelulaPermissao updateCell(String moduloRaw, String nivelRaw, String valorRaw) {
        final Modulo modulo;
        try {
            modulo = Modulo.parse(moduloRaw);
        } catch (IllegalArgumentException ex) {
            throw new PermissaoInvalidaException(ex.getMessage());
        }
        final Role role = parseNivel(nivelRaw);
        final PermissaoNivel valor;
        try {
            valor = PermissaoNivel.parse(valorRaw);
        } catch (IllegalArgumentException ex) {
            throw new PermissaoInvalidaException(ex.getMessage());
        }
        cells.get(modulo).put(role, valor);
        return new Permissoes.CelulaPermissao(modulo.getCode(), role.name(), role.getCode(), valor.getLabel());
    }

    private Role parseNivel(String nivelRaw) {
        if (nivelRaw == null || nivelRaw.isBlank()) {
            throw new PermissaoInvalidaException("Nível inválido: informe 0, 1, 2, 3, 4 ou Admin, Bolsista, Voluntário, Estagiário, Recrutando");
        }
        String normalized = nivelRaw.trim();
        try {
            return Role.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // tenta código numérico abaixo
        }
        try {
            return Role.fromCode(Integer.parseInt(normalized));
        } catch (IllegalArgumentException ex) {
            throw new PermissaoInvalidaException(
                    "Nível inválido: informe 0, 1, 2, 3, 4 ou Admin, Bolsista, Voluntário, Estagiário, Recrutando");
        }
    }
}