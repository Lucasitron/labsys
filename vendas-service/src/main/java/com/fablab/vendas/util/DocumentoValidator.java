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
        return digitos[9] == digitoVerificador(digitos, 9, 10)
                && digitos[10] == digitoVerificador(digitos, 10, 11);
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
        int d1 = simplificar(digitos[0] * 5 + digitos[1] * 4 + digitos[2] * 3 + digitos[3] * 2
                + digitos[4] * 9 + digitos[5] * 8 + digitos[6] * 7 + digitos[7] * 6
                + digitos[8] * 5 + digitos[9] * 4 + digitos[10] * 3 + digitos[11] * 2);
        int d2 = simplificar(digitos[0] * 6 + digitos[1] * 5 + digitos[2] * 4 + digitos[3] * 3
                + digitos[4] * 2 + digitos[5] * 9 + digitos[6] * 8 + digitos[7] * 7
                + digitos[8] * 6 + digitos[9] * 5 + digitos[10] * 4 + digitos[11] * 3
                + digitos[12] * 2);
        return digitos[12] == d1 && digitos[13] == d2;
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

    /** Calcula um dígito verificador sobre os {@code ate} primeiros dígitos. */
    private static int digitoVerificador(int[] digitos, int ate, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < ate; i++) {
            soma += digitos[i] * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Reduz o resto da soma ponderada do CNPJ a um dígito verificador. */
    private static int simplificar(int soma) {
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}