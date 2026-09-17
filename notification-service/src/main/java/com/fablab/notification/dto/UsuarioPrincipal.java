package com.fablab.notification.dto;

import com.fablab.notification.entity.NivelAcesso;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Principal autenticado, extraído do JWT emitido pelo Auth &amp; Identity Service.
 *
 * @param idPessoa    id da pessoa no Pessoas &amp; RH Service (claim {@code id_user})
 * @param nivel       nível de acesso derivado da claim {@code role}
 * @param setor       setor/departamento informado na claim
 * @param authorities autoridades do Spring Security derivadas do nível
 */
public record UsuarioPrincipal(
        Long idPessoa,
        NivelAcesso nivel,
        String setor,
        Collection<? extends GrantedAuthority> authorities) {

    public UsuarioPrincipal {
        authorities = authorities == null || authorities.isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + nivel.name()))
                : List.copyOf(authorities);
    }

    public UsuarioPrincipal(Long idPessoa, NivelAcesso nivel, String setor) {
        this(idPessoa, nivel, setor, null);
    }
}
