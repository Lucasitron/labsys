package com.fablab.vendas.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** DTOs de tags de clientes. */
public final class TagDtos {

    private TagDtos() {
    }

    public record TagRequest(
            @NotBlank(message = "O nome da tag é obrigatório")
            String nome,

            String cor) {
    }

    public record TagResponse(Long id, String nome, String cor) {
    }

    public record TagListaResponse(List<TagResponse> tags) {
    }
}
