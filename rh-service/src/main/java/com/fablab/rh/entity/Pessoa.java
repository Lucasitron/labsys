package com.fablab.rh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pessoa do Fab Lab (professor, aluno ou candidato).
 *
 * <p>Corresponde à tabela {@code pessoa}. O campo {@code id} é usado como
 * referência externa de usuário pelo Auth &amp; Identity Service.</p>
 */
@Entity
@Table(name = "pessoa", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pessoa_matricula", columnNames = "matricula")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pessoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "matricula", nullable = false, unique = true)
    private String matricula;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @Column(name = "contato")
    private String contato;

    @Column(name = "turno")
    private String turno;

    @Column(name = "cpf", length = 11)
    private String cpf;

    @Convert(converter = PessoaStatusConverter.class)
    @Column(name = "status", nullable = false)
    private PessoaStatus status;
}