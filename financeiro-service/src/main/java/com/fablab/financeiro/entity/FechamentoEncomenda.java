package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Fechamento de encomenda: horas estimadas e valor fechado com o cliente.
 *
 * <p>Corresponde à tabela {@code fechamento_encomenda}. Criado em {@code ABERTA}
 * quando o Vendas publica {@code encomenda.criada.event} ou manualmente pelo
 * endpoint. Valores de estimativa e valor são congelados após a criação.
 * {@code horasValidadas} acumula as horas validadas pelo RH.</p>
 */
@Entity
@Table(name = "fechamento_encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FechamentoEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idFechamento;

    @Column(name = "id_encomenda", nullable = false, unique = true)
    private Long idEncomenda;

    @Column(name = "horas_estimadas", precision = 12, scale = 2)
    private BigDecimal horasEstimadas;

    @Column(name = "valor_fechado", nullable = false, precision = 14, scale = 2)
    private BigDecimal valorFechado;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDate dataFechamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusFechamentoEncomenda status;

    @Column(name = "horas_validadas", nullable = false, precision = 14, scale = 2)
    private BigDecimal horasValidadas = BigDecimal.ZERO;

    /**
     * Adiciona horas validadas ao acumulado da encomenda.
     *
     * @param horas quantidade de horas a somar
     */
    public void adicionarHorasValidadas(BigDecimal horas) {
        this.horasValidadas = this.horasValidadas.add(horas);
    }
}