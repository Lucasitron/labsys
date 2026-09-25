package com.fablab.estoque.mapper;

import com.fablab.estoque.dto.FornecedorRequest;
import com.fablab.estoque.dto.FornecedorResponse;
import com.fablab.estoque.entity.Fornecedor;

/**
 * Mapeia entidades {@code Fornecedor} para DTOs e aplica atualizações.
 */
public final class FornecedorMapper {

    private FornecedorMapper() {
    }

    public static Fornecedor toEntity(FornecedorRequest request) {
        Fornecedor fornecedor = new Fornecedor();
        fornecedor.setNome(request.nome());
        fornecedor.setContato(request.contato());
        fornecedor.setCnpj(request.cnpj());
        return fornecedor;
    }

    public static FornecedorResponse toResponse(Fornecedor fornecedor) {
        return new FornecedorResponse(
                fornecedor.getId(),
                fornecedor.getNome(),
                fornecedor.getContato(),
                fornecedor.getCnpj());
    }
}