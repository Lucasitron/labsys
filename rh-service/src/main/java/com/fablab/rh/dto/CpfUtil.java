package com.fablab.rh.dto;

/**
 * Utilitário de CPF (LGPD): normalização, validação de dígitos e máscara.
 *
 * <p>Decisão de retenção (spec omissa — reportada): o CPF é opcional,
 * persistido somente com dígitos e respondido sempre mascarado
 * ({@code ***.***.***-XX}, exibindo apenas os dois últimos dígitos).</p>
 */
public final class CpfUtil {

    private CpfUtil() {
    }

    /** Normaliza para somente dígitos (nulo quando ausente/em branco). */
    public static String normalizar(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }

    /** Se o valor (formatado ou não) possui 11 dígitos e check-digits válidos. */
    public static boolean valido(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        try {
            int d1 = digito(digitos, 9, 10);
            int d2 = digito(digitos, 10, 11);
            return digitos.charAt(9) - '0' == d1 && digitos.charAt(10) - '0' == d2;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static int digito(String digitos, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (digitos.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Máscara LGPD: {@code ***.***.***-XX} (nulo quando ausente). */
    public static String mascarar(String cpfNormalizado) {
        if (cpfNormalizado == null || cpfNormalizado.length() != 11) {
            return null;
        }
        return "***.***.***-" + cpfNormalizado.substring(9);
    }
}
