package com.fablab.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Contratos de configurações do sistema + tokens de integração
 * ({@code /api/configuracoes/sistema}, {@code /api/configuracoes/tokens}).
 *
 * <p>Admin-only server-side. Segredo (chave/hash) NUNCA trafega fora da
 * resposta de criação.</p>
 */
public final class Sistema {

    private Sistema() {
    }

    public record IdentidadeResponse(
            String nomeFablab,
            String logo) {
    }

    /** Resumo público do token: SEM segredo (sem chave, sem hash). */
    public record TokenResumoResponse(
            Long id,
            String nome,
            String prefixo,
            Instant criadoEm,
            Instant ultimoUso,
            boolean revogado) {
    }

    public record SistemaResponse(
            IdentidadeResponse identidade,
            String cadenciaChecklist5S,
            String cadenciaAuditoria5S,
            List<TokenResumoResponse> tokens) {
    }

    public record IdentidadeRequest(

            @NotBlank(message = "nome do laboratório é obrigatório")
            @Size(max = 255, message = "nome do laboratório deve ter no máximo 255 caracteres")
            String nomeFablab,

            @Size(max = 2048, message = "logo deve ter no máximo 2048 caracteres")
            String logo) {
    }

    public record AtualizarSistemaRequest(

            @Valid
            @NotNull(message = "identidade é obrigatória")
            IdentidadeRequest identidade,

            @NotBlank(message = "cadência do checklist 5S é obrigatória")
            @Size(max = 32, message = "cadência do checklist 5S deve ter no máximo 32 caracteres")
            String cadenciaChecklist5S,

            @Size(max = 32, message = "cadência da auditoria 5S deve ter no máximo 32 caracteres")
            String cadenciaAuditoria5S) {
    }

    public record CriarTokenRequest(

            @NotBlank(message = "nome da integração é obrigatório")
            @Size(max = 128, message = "nome da integração deve ter no máximo 128 caracteres")
            String nome) {
    }

    /**
     * Resposta de criação: ÚNICA vez em que a chave em claro é exibida
     * (C-5). Não persistir nem logar.
     */
    public record TokenCriadoResponse(
            Long id,
            String nome,
            String prefixo,
            Instant criadoEm,
            String chave) {
    }
}
