package com.fablab.estoque.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lista de Materiais (BOM) de um produto/serviço.
 *
 * <p>Corresponde à tabela {@code lista_materiais}. O {@code idProdutoServico} é
 * uma referência externa ao produto/serviço no Vendas &amp; CRM ou Produção.</p>
 */
@Entity
@Table(name = "lista_materiais")
public class ListaMateriais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bom")
    private Long id;

    @Column(name = "id_produto_servico", nullable = false)
    private Long idProdutoServico;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "versao", nullable = false)
    private Integer versao;

    @Column(name = "editavel", nullable = false)
    private Boolean editavel;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemBom> itens = new ArrayList<>();

    public ListaMateriais() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdProdutoServico() {
        return idProdutoServico;
    }

    public void setIdProdutoServico(Long idProdutoServico) {
        this.idProdutoServico = idProdutoServico;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getVersao() {
        return versao;
    }

    public void setVersao(Integer versao) {
        this.versao = versao;
    }

    public Boolean getEditavel() {
        return editavel;
    }

    public void setEditavel(Boolean editavel) {
        this.editavel = editavel;
    }

    public List<ItemBom> getItens() {
        return itens;
    }

    public void setItens(List<ItemBom> itens) {
        this.itens = itens;
    }

    public void adicionarItem(ItemBom itemBom) {
        itemBom.setBom(this);
        itens.add(itemBom);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ListaMateriais that = (ListaMateriais) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}