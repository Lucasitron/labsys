package com.fablab.producao.dto;

import com.fablab.producao.entity.SetorMaterial;
import java.math.BigDecimal;

/** Representação de um material de setor. */
public record MaterialSetorResponse(Long idMaterial, String descricao, BigDecimal quantidade) {

    public static MaterialSetorResponse from(SetorMaterial material) {
        return new MaterialSetorResponse(material.getIdMaterial(), material.getDescricao(), material.getQuantidade());
    }
}