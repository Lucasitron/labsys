package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;

/**
 * Visão de uma categoria financeira.
 */
public record CategoriaFinanceiraResponse(
        Long idCategoria,
        String nome,
        TipoCategoriaFinanceira tipo,
        String descricao) {

    public static CategoriaFinanceiraResponse of(CategoriaFinanceira categoria) {
        return new CategoriaFinanceiraResponse(
                categoria.getIdCategoria(),
                categoria.getNome(),
                categoria.getTipo(),
                categoria.getDescricao());
    }
}