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

/** Advertência aplicada a um membro em decorrência de não conformidade. */
@Entity
@Table(name = "advertencia_membro")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AdvertenciaMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idAdvertencia;

    @Column(name = "id_funcionario", nullable = false)
    private Long idFuncionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inspecao")
    private Inspecao5S inspecao;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoAdvertencia tipo;

    @Column(name = "contador", nullable = false)
    private Integer contador;

    @Column(name = "id_admin_registrou")
    private Long idAdminRegistrou;
}