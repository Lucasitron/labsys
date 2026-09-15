package com.fablab.rh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Histórico de alterações de nível de acesso (auditoria).
 *
 * <p>Corresponde à tabela {@code historico_nivel}.</p>
 */
@Entity
@Table(name = "historico_nivel")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HistoricoNivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false)
    private Funcionario funcionario;

    @Convert(converter = NivelAcessoConverter.class)
    @Column(name = "nivel_antigo", nullable = false)
    private NivelAcesso nivelAntigo;

    @Convert(converter = NivelAcessoConverter.class)
    @Column(name = "nivel_novo", nullable = false)
    private NivelAcesso nivelNovo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_admin_alterou", nullable = false)
    private Funcionario admin;

    @Column(name = "data_alteracao", nullable = false)
    private Instant dataAlteracao;
}