package com.fablab.vendas.service;

import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.exception.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Enforcement criador+Admin (D-3): edição/mover/decidir só pelo criador do
 * registro ou Admin; demais recebem 403.
 */
@Component
public class PermissaoUtil {

    private static final String MSG = "Apenas quem criou o registro ou Admin pode alterar. Use Solicitar alteração.";

    /** Lança {@link ForbiddenException} se não for criador nem Admin. */
    public void exigirCriadorOuAdmin(Long criadoPor, VendasPrincipal principal) {
        if (principal.isAdmin()) {
            return;
        }
        if (criadoPor == null || !criadoPor.equals(principal.idPessoa())) {
            throw new ForbiddenException(MSG);
        }
    }
}
