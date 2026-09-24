package com.fablab.auth.service;

import com.fablab.auth.dto.RbacResponse;
import com.fablab.auth.entity.Role;
import java.util.List;
import java.util.Map;
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
}