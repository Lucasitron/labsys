package com.fablab.vendas.util;

/**
 * Validação de documentos brasileiros (CPF e CNPJ) usada no cadastro de
 * clientes, conforme regra de negócio do Vendas &amp; CRM Service.
 */
public final class DocumentoValidator {

    private DocumentoValidator() {
    }

    /** Valida um CPF (11 dígitos, verificadores corretos). */
    public static boolean isCpfValido(String cpf) {
        if (!ehDigitos(cpf, 11)) {
            return false;
        }
        int[] digitos = cpf.chars().map(c -> c - '0').toArray();
        if (todosIguais(digitos)) {
            return false;
        }
        return digitos[9] == calcularDigito(digitos, 9, pesoSemDV())
                && digitos[10] == calcularDigito(digitos, 10, pesoSemDV());
    }

    /** Valida um CNPJ (14 dígitos, verificadores corretos). */
    public static boolean isCnpjValido(String cnpj) {
        if (!ehDigitos(cnpj, 14)) {
            return false;
        }
        int[] digitos = cnpj.chars().map(c -> c - '0').toArray();
        if (todosIguais(digitos)) {
            return false;
        }
        return digitos[12] == calcularDigito(digitos, 12, pesoCnpj(12))
                && digitos[13] == calcularDigito(digitos, 13, pesoCnpj(13));
    }

    private static boolean ehDigitos(String valor, int tamanho) {
        return valor != null && valor.matches("\\d{" + tamanho + "}");
    }

    private static boolean todosIguais(int[] digitos) {
        for (int d : digitos) {
            if (d != digitos[0]) {
                return false;
            }
        }
        return true;
    }

    private static int[] pesoSemDV() {
        return new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2};
    }

    /** Pesos da validação de CNPJ para o DV dado (12 ou 13). */
    private static int[] pesoCnpj(int dv) {
        int[] base = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        if (dv == 12) {
            return base;
        }
        return new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    }

    /** Calcula um dígito verificador a partir dos dígitos e pesos. */
    private static int calcularDigito(int[] digitos, int ate, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += digitos[i] * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}