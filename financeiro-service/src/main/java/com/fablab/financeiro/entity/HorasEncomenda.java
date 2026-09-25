package com.fablab.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Horas validadas acumuladas por (encomenda, funcionário, data), base da mão
 * de obra do custeio. Corresponde à tabela {@code horas_encomenda}.
 */
@Entity
@Table(name = "horas_encomenda")
public class HorasEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horas")
    private Long id;

    @Column(name = "id_encomenda", nullable = false)
    private Integer idEncomenda;

    @Column(name = "id_funcionario", nullable = false)
    private Integer idFuncionario;

    @Column(name = "nivel_acesso")
    private Integer nivelAcesso;

    @Column(name = "horas", nullable = false, precision = 12, scale = 2)
    private BigDecimal horas;

    @Column(name = "data_registro", nullable = false)
    private LocalDate dataRegistro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getIdEncomenda() {
        return idEncomenda;
    }

    public void setIdEncomenda(Integer idEncomenda) {
        this.idEncomenda = idEncomenda;
    }

    public Integer getIdFuncionario() {
        return idFuncionario;
    }

    public void setIdFuncionario(Integer idFuncionario) {
        this.idFuncionario = idFuncionario;
    }

    public Integer getNivelAcesso() {
        return nivelAcesso;
    }

    public void setNivelAcesso(Integer nivelAcesso) {
        this.nivelAcesso = nivelAcesso;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public void setHoras(BigDecimal horas) {
        this.horas = horas;
    }

    public LocalDate getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(LocalDate dataRegistro) {
        this.dataRegistro = dataRegistro;
    }
}
