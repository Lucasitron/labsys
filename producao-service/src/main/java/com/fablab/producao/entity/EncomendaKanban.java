package com.fablab.producao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cartão do Kanban de produção, associado a uma encomenda do Vendas.
 *
 * <p>Usa bloqueio otimista ({@code version}) para evitar movimentações
 * concorrentes.</p>
 */
@Entity
@Table(name = "encomenda_kanban")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EncomendaKanban {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idKanban;

    @Column(name = "id_encomenda", nullable = false, unique = true)
    private Long idEncomenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KanbanStatus status;

    @Column(name = "data_entrada_status", nullable = false)
    private LocalDateTime dataEntradaStatus;

    @Column(name = "id_responsavel")
    private Long idResponsavel;

    @Column(name = "ordem", nullable = false)
    private Integer ordem;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}