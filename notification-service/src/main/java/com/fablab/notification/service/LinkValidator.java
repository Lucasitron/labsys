package com.fablab.notification.service;

import com.fablab.notification.exception.LinkInvalidoException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validação do campo {@code link} das notificações (D-6).
 *
 * <p>Allowlist mínima: caminho interno (ex. {@code /notificacoes/12}) ou URL
 * absoluta {@code http(s)} com host. Qualquer outro formato (inclusive
 * {@code javascript:} e {@code data:}) é rejeitado com HTTP 422.
 */
public final class LinkValidator {

    private LinkValidator() {
    }

    public static boolean isValido(String link) {
        if (link == null || link.isBlank()) {
            return true;
        }
        String valor = link.strip();
        if (valor.startsWith("/")) {
            return !valor.contains(" ") && !valor.contains("\\");
        }
        try {
            URI uri = new URI(valor);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static void assertValido(String link) {
        if (!isValido(link)) {
            throw new LinkInvalidoException(
                    "Link da notificação inválido: use um caminho interno (/...) ou URL http(s)");
        }
    }
}
