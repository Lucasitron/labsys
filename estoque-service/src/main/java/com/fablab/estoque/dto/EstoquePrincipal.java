package com.fablab.estoque.dto;

import com.fablab.estoque.entity.NivelAcesso;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Principal autenticado, extraído do JWT emitido pelo Auth &amp; Identity Service.
 *
 * @param idPessoa     id da pessoa no Pessoas &amp; RH Service (claim {@code id_user})
 * @param nivel        nível de acesso derivado da claim {@code role}
 * @param setor        setor/departamento informado na claim
 * @param authorities  autoridades do Spring Security derivadas do nível
 */
public record EstoquePrincipal(
        Long idPessoa,
        NivelAcesso nivel,
        String setor,
        Collection<? extends GrantedAuthority> authorities) {

    public EstoquePrincipal {
        authorities = authorities == null || authorities.isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + nivel.name()))
                : List.copyOf(authorities);
    }

    public EstoquePrincipal(Long idPessoa, NivelAcesso nivel, String setor) {
        this(idPessoa, nivel, setor, null);
    }

    /** Se o principal é um Admin (nível 0). */
    public boolean isAdmin() {
        return nivel == NivelAcesso.ADMIN;
    }
}