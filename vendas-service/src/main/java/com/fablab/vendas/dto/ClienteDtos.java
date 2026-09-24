package com.fablab.vendas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * DTOs de clientes (documento mascarado por padrão — LGPD, D-7).
 */
public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(
            @NotNull(message = "O tipo de pessoa é obrigatório (PF ou PJ)")
            @Pattern(regexp = "PF|PJ", message = "O tipo de pessoa deve ser PF ou PJ")
            String tipoPessoa,

            @NotBlank(message = "O nome ou razão social é obrigatório")
            String nomeRazaoSocial,

            @NotBlank(message = "O CPF ou CNPJ é obrigatório")
            String cpfCnpj,

            @Email(message = "O e-mail informado é inválido")
            String email,

            String telefone,
            String endereco) {
    }

    public record TagResumo(Long id, String nome, String cor) {
    }

    public record ClienteResponse(
            Long id,
            String tipoPessoa,
            String nomeRazaoSocial,
            String documento,
            String email,
            String telefone,
            String endereco,
            LocalDate dataCadastro,
            List<TagResumo> tags,
            Long criadoPor) {
    }

    public record ClienteDetalheResponse(
            Long id,
            String tipoPessoa,
            String nomeRazaoSocial,
            String documento,
            String email,
            String telefone,
            String endereco,
            LocalDate dataCadastro,
            List<TagResumo> tags,
            Long criadoPor,
            Map<String, Object> indicadores) {
    }

    public record Paginacao(int page, int pageSize, long totalItems, int totalPages) {
    }

    public record ClientePaginaResponse(
            List<ClienteResponse> clientes,
            Paginacao pagination,
            Map<String, List<Map<String, Object>>> filters) {
    }

    public record VinculoTagRequest(
            @NotNull(message = "O id da tag é obrigatório")
            Long tagId) {
    }

    public record BulkTagRequest(
            @NotNull(message = "A lista de clientes é obrigatória")
            List<Long> clienteIds,

            @NotNull(message = "O id da tag é obrigatório")
            Long tagId) {
    }
}
