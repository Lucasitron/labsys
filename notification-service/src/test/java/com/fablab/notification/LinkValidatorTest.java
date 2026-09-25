package com.fablab.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.notification.dto.ErrorResponse;
import com.fablab.notification.exception.GlobalExceptionHandler;
import com.fablab.notification.exception.LinkInvalidoException;
import com.fablab.notification.service.LinkValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * D-6 (validação de link, 422) — unidade.
 */
class LinkValidatorTest {

    @Test
    void aceitaAusenteRelativoEHttp() {
        assertThat(LinkValidator.isValido(null)).isTrue();
        assertThat(LinkValidator.isValido("  ")).isTrue();
        assertThat(LinkValidator.isValido("/notificacoes/12")).isTrue();
        assertThat(LinkValidator.isValido("https://fablab.local/avisos/1")).isTrue();
        assertThat(LinkValidator.isValido("http://localhost:4173/notificacoes")).isTrue();
    }

    @Test
    void rejeitaEsquemasPerigososEFormatoInvalido() {
        assertThat(LinkValidator.isValido("javascript:alert(1)")).isFalse();
        assertThat(LinkValidator.isValido("data:text/html;base64,xxx")).isFalse();
        assertThat(LinkValidator.isValido("ftp://arquivos/oferta")).isFalse();
        assertThat(LinkValidator.isValido("https://")).isFalse();
        assertThat(LinkValidator.isValido("/caminho com espaco")).isFalse();
    }

    @Test
    void assertValidoLancaExcecaoEmPortugues() {
        assertThatThrownBy(() -> LinkValidator.assertValido("javascript:alert(1)"))
                .isInstanceOf(LinkInvalidoException.class)
                .hasMessageContaining("Link da notificação inválido");
    }

    @Test
    void handlerMapeiaPara422SemDetalheTecnico() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/notifications");
        ResponseEntity<ErrorResponse> response = handler.handleLinkInvalido(
                new LinkInvalidoException("Link da notificação inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).contains("Link da notificação inválido");
        assertThat(response.getBody().message()).doesNotContain("Exception");
    }
}
