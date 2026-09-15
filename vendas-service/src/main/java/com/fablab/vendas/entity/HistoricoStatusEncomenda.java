package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de auditoria das movimentações de status de uma encomenda no Kanban.
 *
 * <p>Corresponde à tabela {@code historico_status_encomenda}.</p>
 */
@Entity
@Table(name = "historico_status_encomenda")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoStatusEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_encomenda", nullable = false)
    private Encomenda encomenda;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusKanban statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusKanban statusNovo;

    @Column(name = "data_alteracao", nullable = false)
    private LocalDateTime dataAlteracao;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "observacao", length = 500)
    private String observacao;
}