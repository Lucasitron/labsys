package com.fablab.vendas.dto;

import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.ClienteTag;
import com.fablab.vendas.entity.TipoPessoa;
import java.time.LocalDate;
import java.util.List;

/**
 * Resposta de cliente, incluindo as tags associadas.
 */
public record ClienteResponse(
        Long id,
        TipoPessoa tipoPessoa,
        String nomeRazaoSocial,
        String cpfCnpj,
        String email,
        String telefone,
        String endereco,
        LocalDate dataCadastro,
        List<TagClienteResponse> tags) {

    public static ClienteResponse of(Cliente cliente) {
        List<TagClienteResponse> tags = cliente.getTags() == null ? List.of()
                : cliente.getTags().stream()
                        .map(ClienteTag::getTag)
                        .map(TagClienteResponse::of)
                        .toList();
        return new ClienteResponse(
                cliente.getId(),
                cliente.getTipoPessoa(),
                cliente.getNomeRazaoSocial(),
                cliente.getCpfCnpj(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getEndereco(),
                cliente.getDataCadastro(),
                tags);
    }
}