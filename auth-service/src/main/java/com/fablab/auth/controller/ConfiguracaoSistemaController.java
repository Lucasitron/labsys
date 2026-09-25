package com.fablab.auth.controller;

import com.fablab.auth.dto.ErrorResponse;
import com.fablab.auth.dto.Sistema;
import com.fablab.auth.service.SistemaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parâmetros globais do sistema (Admin-only, server-side).
 *
 * <p>Persiste identidade + cadências 5S (alimenta Produção por leitura).
 * Validação PT; 422 se inválido.</p>
 */
@RestController
@RequestMapping("/api/configuracoes/sistema")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracaoSistemaController {

    private final SistemaService sistemaService;

    public ConfiguracaoSistemaController(SistemaService sistemaService) {
        this.sistemaService = sistemaService;
    }

    @GetMapping
    public ResponseEntity<Sistema.SistemaResponse> obter() {
        return ResponseEntity.ok(sistemaService.obter());
    }

    @PutMapping
    public ResponseEntity<Sistema.SistemaResponse> atualizar(
            @Valid @RequestBody Sistema.AtualizarSistemaRequest request) {
        return ResponseEntity.ok(sistemaService.atualizar(request));
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
                "Parâmetros do sistema inválidos",
                request.getRequestURI(),
                fields);
        return ResponseEntity.unprocessableEntity().body(body);
    }
}
