package com.fablab.vendas.unit;

import com.fablab.vendas.service.DocumentoUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validação de CPF/CNPJ e máscara LGPD (D-7). */
class DocumentoUtilTest {

    private final DocumentoUtil documentoUtil = new DocumentoUtil();

    @Test
    void cpfValido() {
        assertTrue(documentoUtil.valido("529.982.247-25"));
        assertTrue(documentoUtil.valido("52998224725"));
    }

    @Test
    void cpfInvalido() {
        assertFalse(documentoUtil.valido("111.111.111-11"));
        assertFalse(documentoUtil.valido("123.456.789-00"));
        assertFalse(documentoUtil.valido("abc"));
        assertFalse(documentoUtil.valido(null));
    }

    @Test
    void cnpjValido() {
        assertTrue(documentoUtil.valido("11.222.333/0001-81"));
        assertTrue(documentoUtil.valido("11222333000181"));
    }

    @Test
    void cnpjInvalido() {
        assertFalse(documentoUtil.valido("11.111.111/1111-11"));
        assertFalse(documentoUtil.valido("11222333000100"));
    }

    @Test
    void mascaraPorPadrao() {
        String mascarado = documentoUtil.mascarar("529.982.247-25");
        assertTrue(mascarado.startsWith("***"));
        assertTrue(mascarado.endsWith("**"));
        assertFalse(mascarado.contains("529"));
        String cnpjMascarado = documentoUtil.mascarar("11.222.333/0001-81");
        assertTrue(cnpjMascarado.endsWith("**"));
        assertFalse(cnpjMascarado.contains("11222333"));
    }
}
