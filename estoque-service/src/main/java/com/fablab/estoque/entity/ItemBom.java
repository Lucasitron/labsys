package com.fablab.estoque.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item de uma Lista de Materiais (BOM).
 *
 * <p>Corresponde à tabela {@code item_bom}. {@code quantidadeReal} é preenchida
 * durante a produção quando o consumo efetivo é registrado.</p>
 */
@Entity
@Table(name = "item_bom", uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_bom_bom_item", columnNames = {"id_bom", "id_item"})
})
public class ItemBom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_bom")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_bom", nullable = false)
    private ListaMateriais bom;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    @Column(name = "quantidade_prevista", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidadePrevista;

    @Column(name = "quantidade_real", precision = 12, scale = 2)
    private BigDecimal quantidadeReal;

    public ItemBom() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ListaMateriais getBom() {
        return bom;
    }

    public void setBom(ListaMateriais bom) {
        this.bom = bom;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BigDecimal getQuantidadePrevista() {
        return quantidadePrevista;
    }

    public void setQuantidadePrevista(BigDecimal quantidadePrevista) {
        this.quantidadePrevista = quantidadePrevista;
    }

    public BigDecimal getQuantidadeReal() {
        return quantidadeReal;
    }

    public void setQuantidadeReal(BigDecimal quantidadeReal) {
        this.quantidadeReal = quantidadeReal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ItemBom itemBom = (ItemBom) o;
        return Objects.equals(id, itemBom.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}