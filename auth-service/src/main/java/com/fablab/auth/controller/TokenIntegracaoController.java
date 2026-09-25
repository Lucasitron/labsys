package com.fablab.auth.controller;

import com.fablab.auth.dto.ErrorResponse;
import com.fablab.auth.dto.Sistema;
import com.fablab.auth.service.TokenIntegracaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tokens de integração (Admin-only, server-side, C-5).
 *
 * <p>Lista sem segredo; criação exibe a chave UMA vez; revogação é soft;
 * 404 se inexistente.</p>
 */
@RestController
@RequestMapping("/api/configuracoes/tokens")
@PreAuthorize("hasRole('ADMIN')")
public class TokenIntegracaoController {

    private final TokenIntegracaoService tokenService;

    public TokenIntegracaoController(TokenIntegracaoService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    public ResponseEntity<List<Sistema.TokenResumoResponse>> listar() {
        return ResponseEntity.ok(tokenService.listarAtivos());
    }

    @PostMapping
    public ResponseEntity<Sistema.TokenCriadoResponse> criar(
            @Valid @RequestBody Sistema.CriarTokenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenService.criar(request.nome()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revogar(@PathVariable Long id) {
        tokenService.revogar(id);
        return ResponseEntity.noContent().build();
    }

    /** Validação Bean Validation responde 422 PT neste recurso. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                "Dados do token inválidos",
                request.getRequestURI(),
                fields);
        return ResponseEntity.unprocessableEntity().body(body);
    }
}
