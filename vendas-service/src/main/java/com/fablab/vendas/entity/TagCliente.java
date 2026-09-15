package com.fablab.vendas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tag de segmentação de clientes (ex: VIP, 3D, Laser).
 *
 * <p>Corresponde à tabela {@code tag_cliente}.</p>
 */
@Entity
@Table(name = "tag_cliente")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TagCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 50)
    private String nome;

    @Column(name = "cor", length = 20)
    private String cor;
}