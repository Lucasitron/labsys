package com.fablab.producao.entity;

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
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Auditoria de um projeto de mesa, indicando se permanece ativo ou foi abandonado. */
@Entity
@Table(name = "auditoria_projeto_mesa")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditoriaProjetoMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idAuditoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_projeto_mesa", nullable = false)
    private ProjetoMesa projetoMesa;

    @Column(name = "data_auditoria", nullable = false)
    private LocalDate dataAuditoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false, length = 20)
    private StatusProjetoMesa resultado;

    @Column(name = "acao_tomada", length = 500)
    private String acaoTomada;

    @Column(name = "id_admin_responsavel", nullable = false)
    private Long idAdminResponsavel;
}