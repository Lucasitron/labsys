package com.fablab.producao.service;

import com.fablab.producao.dto.ProducaoPrincipal;
import com.fablab.producao.entity.NivelAcesso;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Apoia o RBAC no nível de serviço: identifica o usuário autenticado e decide se
 * ele pode editar um recurso do qual é responsável.
 *
 * <p>Usado via SpEL ({@code @acesso.podeEditar(...)}) e diretamente nos
 * serviços. Quando não há principal (testes unitários ou chamadas internas
 * disparadas por eventos), a edição é permitida.</p>
 */
@Service("acesso")
public class AcessoService {

    /** Retorna o principal autenticado ou {@code null} se não houver contexto. */
    public ProducaoPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ProducaoPrincipal principal)) {
            return null;
        }
        return principal;
    }

    /** Retorna o id do usuário autenticado, ou {@code null}. */
    public Long idUsuario() {
        ProducaoPrincipal principal = principal();
        return principal == null ? null : principal.idPessoa();
    }

    /** Indica se o usuário é administrador. */
    public boolean isAdmin() {
        ProducaoPrincipal principal = principal();
        return principal != null && principal.nivel() == NivelAcesso.ADMIN;
    }

    /**
     * Verifica se o usuário pode editar um recurso cujo responsável é
     * {@code idResponsavel}. ADMIN pode sempre; demais níveis só se forem o
     * responsável (ou se não houver principal).
     */
    public boolean podeEditar(Long idResponsavel) {
        ProducaoPrincipal principal = principal();
        if (principal == null || principal.nivel() == NivelAcesso.ADMIN) {
            return true;
        }
        return idResponsavel != null && idResponsavel.equals(principal.idPessoa());
    }
}