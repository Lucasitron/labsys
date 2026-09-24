package com.fablab.rh.dto;

import com.fablab.rh.entity.NivelAcesso;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Principal autenticado, extraído do JWT emitido pelo Auth &amp; Identity Service.
 *
 * @param idPessoa       id da pessoa no Pessoas &amp; RH Service (claim {@code id_user})
 * @param idFuncionario  id do funcionário vinculado à pessoa (quando existir)
 * @param nivel          nível de acesso derivado da claim {@code role}
 * @param setor          setor/departamento informado na claim
 * @param authorities    autoridades do Spring Security derivadas do nível
 */
public record RhPrincipal(
        Long idPessoa,
        Long idFuncionario,
        NivelAcesso nivel,
        String setor,
        Collection<? extends GrantedAuthority> authorities) {

    public RhPrincipal {
        authorities = authorities == null || authorities.isEmpty()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + nivel.name()))
                : List.copyOf(authorities);
    }

    public RhPrincipal(Long idPessoa, Long idFuncionario, NivelAcesso nivel, String setor) {
        this(idPessoa, idFuncionario, nivel, setor, null);
    }

    /** Se o principal é um Admin (nível 0). */
    public boolean isAdmin() {
        return nivel == NivelAcesso.ADMIN;
    }
}