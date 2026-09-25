package com.fablab.producao.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Inspeção 5S de um setor. */
@Entity
@Table(name = "inspecao_5s")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Inspecao5S {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long idInspecao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_setor", nullable = false)
    private Setor setor;

    @Column(name = "id_inspetor", nullable = false)
    private Long idInspetor;

    @Column(name = "data_inspecao", nullable = false)
    private LocalDate dataInspecao;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno", nullable = false, length = 10)
    private TurnoInspecao turno;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusInspecao status;

    @Column(name = "observacoes", length = 1000)
    private String observacoes;

    @OneToMany(mappedBy = "inspecao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemInspecao5S> itens = new ArrayList<>();

    /** Adiciona um item à inspeção, mantendo a relação bidirecional. */
    public void adicionarItem(ItemInspecao5S item) {
        item.setInspecao(this);
        this.itens.add(item);
    }

    /** Recalcula o status consolidado da inspeção a partir dos itens. */
    public void recalcularStatus() {
        this.status = itens.stream().anyMatch(i -> Boolean.FALSE.equals(i.getConforme()))
                ? StatusInspecao.NAO_CONFORME
                : StatusInspecao.OK;
    }
}