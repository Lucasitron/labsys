package com.fablab.financeiro.dto;

import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/** DTOs de doações e recursos de projetos. */
public final class DoacaoDtos {

    private DoacaoDtos() {
    }

    public record DoacaoRequest(
            @NotNull(message = "O tipo é obrigatório (DOACAO ou PROJETO)")
            TipoDoacao tipo,
            @NotBlank(message = "A origem (doador/órgão) é obrigatória")
            @Size(max = 200, message = "A origem deve ter no máximo 200 caracteres")
            String origem,
            @NotNull(message = "O valor é obrigatório")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
            BigDecimal valor,
            @NotNull(message = "A data de recebimento é obrigatória")
            LocalDate dataRecebimento,
            Integer idProjetoAssociado) {
    }

    public record DoacaoResponse(
            Long id,
            TipoDoacao tipo,
            String origem,
            BigDecimal valor,
            LocalDate dataRecebimento,
            Integer idProjetoAssociado) {
        public static DoacaoResponse of(DoacaoRecurso entity) {
            return new DoacaoResponse(entity.getId(), entity.getTipo(), entity.getOrigem(),
                    entity.getValor(), entity.getDataRecebimento(), entity.getIdProjetoAssociado());
        }
    }
}
