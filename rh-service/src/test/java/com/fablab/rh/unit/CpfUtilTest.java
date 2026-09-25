package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fablab.rh.dto.CpfUtil;
import org.junit.jupiter.api.Test;

class CpfUtilTest {

    @Test
    void aceitaCpfValidoFormatadoOuSoDigitos() {
        assertThat(CpfUtil.valido("529.982.247-25")).isTrue();
        assertThat(CpfUtil.valido("52998224725")).isTrue();
    }

    @Test
    void rejeitaCpfInvalido() {
        assertThat(CpfUtil.valido("111.111.111-11")).isFalse();
        assertThat(CpfUtil.valido("123")).isFalse();
        assertThat(CpfUtil.valido(null)).isFalse();
        assertThat(CpfUtil.valido("")).isFalse();
        assertThat(CpfUtil.valido("529.982.247-26")).isFalse();
    }

    @Test
    void mascaraExibeSoOsDoisUltimosDigitos() {
        assertThat(CpfUtil.mascarar("52998224725")).isEqualTo("***.***.***-25");
        assertThat(CpfUtil.mascarar(null)).isNull();
    }

    @Test
    void normalizaParaSoDigitos() {
        assertThat(CpfUtil.normalizar("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfUtil.normalizar(null)).isNull();
        assertThat(CpfUtil.normalizar("  ")).isNull();
    }
}
