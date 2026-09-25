package com.fablab.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parâmetro global do sistema (KV), dono {@code auth-service}.
 *
 * <p>Chaves: {@code identidade.nomeFablab}, {@code identidade.logo},
 * {@code cadencia.checklist5S}, {@code cadencia.auditoria5S}. Alimenta
 * Produção (cadência 5S) — só persistência, sem chamada a outro service.</p>
 */
@Entity
@Table(name = "parametro_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ParametroSistema {

    @Id
    @Column(name = "chave", nullable = false, length = 128)
    private String chave;

    @Column(name = "valor", nullable = false, length = 2048)
    private String valor;

    public ParametroSistema(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }
}
