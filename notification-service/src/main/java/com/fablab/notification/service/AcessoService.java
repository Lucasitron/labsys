package com.fablab.notification.service;

import com.fablab.notification.dto.UsuarioPrincipal;
import com.fablab.notification.entity.NivelAcesso;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Apoia o RBAC no nível de serviço: identifica o usuário autenticado e decide se
 * ele pode acessar notificações das quais é destinatário.
 *
 * <p>Quando não há principal (chamadas internas disparadas por eventos), o
 * acesso é permitido.</p>
 */
@Service("acesso")
public class AcessoService {

    /** Retorna o principal autenticado ou {@code null} se não houver contexto. */
    public UsuarioPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return null;
        }
        return principal;
    }

    /** Retorna o id do usuário autenticado, ou {@code null}. */
    public Long idUsuario() {
        UsuarioPrincipal principal = principal();
        return principal == null ? null : principal.idPessoa();
    }

    /** Indica se o usuário é administrador. */
    public boolean isAdmin() {
        UsuarioPrincipal principal = principal();
        return principal != null && principal.nivel() == NivelAcesso.ADMIN;
    }

    /**
     * Verifica se o usuário pode acessar uma notificação cujo destinatário é
     * {@code idDestinatario}. ADMIN pode sempre; demais níveis só se forem o
     * destinatário (ou se não houver principal).
     */
    public boolean podeAcessar(Long idDestinatario) {
        UsuarioPrincipal principal = principal();
        if (principal == null || principal.nivel() == NivelAcesso.ADMIN) {
            return true;
        }
        return idDestinatario != null && idDestinatario.equals(principal.idPessoa());
    }
}
