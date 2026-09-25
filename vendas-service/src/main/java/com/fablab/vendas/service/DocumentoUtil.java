package com.fablab.vendas.service;

import org.springframework.stereotype.Component;

/**
 * Validação de CPF/CNPJ (dígitos verificadores) e máscara LGPD (D-7: documento
 * mascarado por padrão nas respostas).
 */
@Component
public class DocumentoUtil {

    /** Valida CPF (11 dígitos) ou CNPJ (14 dígitos) pelos dígitos verificadores. */
    public boolean valido(String documento) {
        if (documento == null) {
            return false;
        }
        String digitos = somenteDigitos(documento);
        if (digitos.length() == 11) {
            return cpfValido(digitos);
        }
        if (digitos.length() == 14) {
            return cnpjValido(digitos);
        }
        return false;
    }

    /** Normaliza para somente dígitos (persistência). */
    public String somenteDigitos(String documento) {
        return documento == null ? null : documento.replaceAll("\\D", "");
    }

    /**
     * Máscara LGPD: exibe só os limites do documento
     * ({@code ***.000.42*-**} / {@code **.000.000/0042-**}).
     */
    public String mascarar(String documento) {
        String digitos = somenteDigitos(documento);
        if (digitos == null) {
            return null;
        }
        if (digitos.length() == 11) {
            return "***." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-**";
        }
        if (digitos.length() == 14) {
            return "**. " + digitos.substring(2, 5) + "." + digitos.substring(5, 8)
                    + "/" + digitos.substring(8, 12) + "-**";
        }
        return "***";
    }

    private boolean todosIguais(String digitos) {
        return digitos.chars().distinct().count() == 1;
    }

    private boolean cpfValido(String cpf) {
        if (todosIguais(cpf)) {
            return false;
        }
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int dv1 = 11 - (soma % 11);
        dv1 = dv1 >= 10 ? 0 : dv1;
        if (dv1 != cpf.charAt(9) - '0') {
            return false;
        }
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        int dv2 = 11 - (soma % 11);
        dv2 = dv2 >= 10 ? 0 : dv2;
        return dv2 == cpf.charAt(10) - '0';
    }

    private boolean cnpjValido(String cnpj) {
        if (todosIguais(cnpj)) {
            return false;
        }
        int[] peso1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] peso2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < 12; i++) {
            soma += (cnpj.charAt(i) - '0') * peso1[i];
        }
        int dv1 = soma % 11;
        dv1 = dv1 < 2 ? 0 : 11 - dv1;
        if (dv1 != cnpj.charAt(12) - '0') {
            return false;
        }
        soma = 0;
        for (int i = 0; i < 13; i++) {
            soma += (cnpj.charAt(i) - '0') * peso2[i];
        }
        int dv2 = soma % 11;
        dv2 = dv2 < 2 ? 0 : 11 - dv2;
        return dv2 == cnpj.charAt(13) - '0';
    }
}
