package com.fablab.producao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Projeto individual em uma mesa, com QR Code para totem. */
@Entity
@Table(name = "projeto_mesa")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjetoMesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idProjetoMesa;

    @Column(name = "id_funcionario", nullable = false)
    private Long idFuncionario;

    @Column(name = "id_mesa", nullable = false)
    private Long idMesa;

    @Column(name = "nome_projeto", nullable = false, length = 150)
    private String nomeProjeto;

    @Column(name = "tipo_projeto", length = 100)
    private String tipoProjeto;

    @Column(name = "prazo_execucao")
    private LocalDate prazoExecucao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_ultima_evolucao")
    private LocalDate dataUltimaEvolucao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusProjetoMesa status;

    @Column(name = "qr_code_totem", length = 500)
    private String qrCodeTotem;
}