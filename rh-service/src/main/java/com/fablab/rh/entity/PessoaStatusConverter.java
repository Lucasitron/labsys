package com.fablab.rh.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte {@link PessoaStatus} para o código inteiro (0-2).
 */
@Converter
public class PessoaStatusConverter implements AttributeConverter<PessoaStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PessoaStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public PessoaStatus convertToEntityAttribute(Integer code) {
        if (code == null) {
            return null;
        }
        for (PessoaStatus status : PessoaStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de pessoa inválido: " + code);
    }
}