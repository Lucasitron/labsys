package com.fablab.rh.entity;

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

/**
 * Processo seletivo de um candidato.
 *
 * <p>Corresponde à tabela {@code processo_seletivo}.</p>
 */
@Entity
@Table(name = "processo_seletivo")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProcessoSeletivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_candidato", nullable = false)
    private Pessoa candidato;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tutor", nullable = false)
    private Funcionario tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_processo", nullable = false, length = 32)
    private StatusProcesso statusProcesso;

    @Column(name = "data_inscricao", nullable = false)
    private LocalDate dataInscricao;

    @Column(name = "resultado_final")
    private String resultadoFinal;
}