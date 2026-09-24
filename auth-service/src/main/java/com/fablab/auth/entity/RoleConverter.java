package com.fablab.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte {@link Role} para o código inteiro (0-4) definido na documentação técnica.
 */
@Converter
public class RoleConverter implements AttributeConverter<Role, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Role role) {
        return role == null ? null : role.getCode();
    }

    @Override
    public Role convertToEntityAttribute(Integer code) {
        return code == null ? null : Role.fromCode(code);
    }
}