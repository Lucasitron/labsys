package com.fablab.rh.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte {@link NivelAcesso} para o código inteiro (0-4).
 */
@Converter
public class NivelAcessoConverter implements AttributeConverter<NivelAcesso, Integer> {

    @Override
    public Integer convertToDatabaseColumn(NivelAcesso nivel) {
        return nivel == null ? null : nivel.getCode();
    }

    @Override
    public NivelAcesso convertToEntityAttribute(Integer code) {
        return code == null ? null : NivelAcesso.fromCode(code);
    }
}