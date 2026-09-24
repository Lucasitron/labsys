package com.fablab.auth.dto;

import com.fablab.auth.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Principal do usuário autenticado, extraído do token JWT.
 *
 * @param loginId     id interno da credencial (tabela {@code login})
 * @param idUser      id externo do usuário (Pessoas &amp; RH Service)
 * @param role        papel/função do usuário
 * @param setor       setor/departamento do usuário
 * @param authorities autoridades do Spring Security derivadas do papel
 */
public record LoginPrincipal(
        Long loginId,
        Long idUser,
        Role role,
        String setor,
        Collection<? extends GrantedAuthority> authorities) {

    public LoginPrincipal {
        authorities = authorities == null || authorities.isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                : List.copyOf(authorities);
    }

    /**
     * Cria o principal com as autoridades derivadas automaticamente do papel.
     */
    public LoginPrincipal(Long loginId, Long idUser, Role role, String setor) {
        this(loginId, idUser, role, setor, null);
    }
}