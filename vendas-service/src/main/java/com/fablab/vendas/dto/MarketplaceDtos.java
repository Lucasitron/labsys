package com.fablab.vendas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** DTOs de registro manual de marketplace. */
public final class MarketplaceDtos {

    private MarketplaceDtos() {
    }

    public record MarketplaceRequest(
            @NotNull(message = "A encomenda vinculada é obrigatória")
            Long encomendaId,

            @NotBlank(message = "A plataforma é obrigatória")
            String plataforma,

            @NotBlank(message = "O código externo do pedido é obrigatório")
            String codigoExterno,

            @NotNull(message = "A data da venda é obrigatória")
            LocalDate dataVenda,

            @NotNull(message = "O valor da taxa é obrigatório (em R$, nunca percentual)")
            @DecimalMin(value = "0.0", message = "A taxa não pode ser negativa")
            BigDecimal valorTaxa) {
    }

    public record MarketplaceResponse(
            Long id,
            Long encomendaId,
            String encomendaCodigo,
            String plataforma,
            String codigoExterno,
            String clienteNome,
            BigDecimal valorBruto,
            BigDecimal valorTaxa,
            BigDecimal valorLiquido,
            LocalDate dataVenda) {
    }

    public record MarketplaceListaResponse(
            List<MarketplaceResponse> registros,
            Map<String, BigDecimal> totais) {
    }
}
