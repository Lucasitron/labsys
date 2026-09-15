package com.fablab.vendas.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fablab.vendas.util.DocumentoValidator;
import org.junit.jupiter.api.Test;

class DocumentoValidatorTest {

    @Test
    void cpfValido() {
        assertTrue(DocumentoValidator.isCpfValido("52998224725"));
        assertTrue(DocumentoValidator.isCpfValido("11144477735"));
    }

    @Test
    void cpfInvalido() {
        assertFalse(DocumentoValidator.isCpfValido("11111111111"));
        assertFalse(DocumentoValidator.isCpfValido("12345678900"));
        assertFalse(DocumentoValidator.isCpfValido("12345"));
        assertFalse(DocumentoValidator.isCpfValido("123456789012"));
        assertFalse(DocumentoValidator.isCpfValido(null));
    }

    @Test
    void cnpjValido() {
        assertTrue(DocumentoValidator.isCnpjValido("11222333000181"));
    }

    @Test
    void cnpjInvalido() {
        assertFalse(DocumentoValidator.isCnpjValido("11111111111111"));
        assertFalse(DocumentoValidator.isCnpjValido("12345"));
        assertFalse(DocumentoValidator.isCnpjValido(null));
    }
}