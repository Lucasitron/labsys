package com.fablab.vendas.dto;

import com.fablab.vendas.entity.RegistroMarketplace;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resposta de registro de venda em marketplace.
 */
public record MarketplaceResponse(
        Long id,
        Long idEncomenda,
        String plataforma,
        String codigoExterno,
        LocalDate dataVenda,
        BigDecimal valorTaxa) {

    public static MarketplaceResponse of(RegistroMarketplace registro) {
        return new MarketplaceResponse(
                registro.getId(),
                registro.getEncomenda().getId(),
                registro.getPlataforma(),
                registro.getCodigoExterno(),
                registro.getDataVenda(),
                registro.getValorTaxa());
    }
}