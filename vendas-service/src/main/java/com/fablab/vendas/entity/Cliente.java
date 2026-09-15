package com.fablab.vendas.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente cadastrado no CRM (Pessoa Física ou Jurídica).
 *
 * <p>Corresponde à tabela {@code cliente}.</p>
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false, length = 2)
    private TipoPessoa tipoPessoa;

    @Column(name = "nome_razao_social", nullable = false, length = 200)
    private String nomeRazaoSocial;

    @Column(name = "cpf_cnpj", nullable = false, unique = true, length = 20)
    private String cpfCnpj;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "endereco", length = 300)
    private String endereco;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClienteTag> tags = new ArrayList<>();

    /** Associa uma tag ao cliente (cria o vínculo em {@code ClienteTag}). */
    public void adicionarTag(TagCliente tag) {
        tags.add(new ClienteTag(this, tag));
    }
}