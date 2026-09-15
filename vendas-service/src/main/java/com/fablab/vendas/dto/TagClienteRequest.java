package com.fablab.vendas.dto;

import com.fablab.vendas.entity.TagCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de criação de tag de cliente.
 */
public record TagClienteRequest(
        @NotBlank @Size(max = 50) String nome,
        @Size(max = 20) String cor) {
}