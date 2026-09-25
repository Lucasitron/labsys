package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs de categorias financeiras (receita/despesa). */
public final class CategoriaDtos {

    private CategoriaDtos() {
    }

    public record CategoriaRequest(
            @NotBlank(message = "O nome da categoria é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,
            @NotNull(message = "O tipo da categoria é obrigatório (RECEITA ou DESPESA)")
            TipoCategoria tipo,
            @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
            String descricao) {
    }

    public record CategoriaResponse(Long id, String nome, TipoCategoria tipo, String descricao) {
        public static CategoriaResponse of(CategoriaFinanceira entity) {
            return new CategoriaResponse(entity.getId(), entity.getNome(), entity.getTipo(), entity.getDescricao());
        }
    }
}
