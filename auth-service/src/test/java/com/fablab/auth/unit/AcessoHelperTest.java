package com.fablab.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fablab.auth.service.AcessoHelper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Regras de acesso por módulo derivadas do nível (RBAC).
 */
class AcessoHelperTest {

    @Test
    void adminEditaTodosOsModulos() {
        Map<String, String> acessos = AcessoHelper.acessosPorRole(0);
        assertThat(acessos).allSatisfy((modulo, acesso) -> assertThat(acesso).isEqualTo("edit"));
    }

    @Test
    void bolsistaEditaRhEVisualizaDemais() {
        Map<String, String> acessos = AcessoHelper.acessosPorRole(1);
        assertThat(acessos.get("rh")).isEqualTo("edit");
        assertThat(acessos.get("vendas")).isEqualTo("view");
        assertThat(acessos.get("financeiro")).isNull();
    }

    @Test
    void estagiarioApenasVisualiza() {
        Map<String, String> acessos = AcessoHelper.acessosPorRole(3);
        assertThat(acessos.get("rh")).isEqualTo("view");
        assertThat(acessos.get("estoque")).isEqualTo("view");
        assertThat(acessos.get("financeiro")).isNull();
        assertThat(acessos.get("producao")).isEqualTo("view");
    }

    @Test
    void recrutandoRuãoAcessaModulosInternos() {
        Map<String, String> acessos = AcessoHelper.acessosPorRole(4);
        assertThat(acessos.get("rh")).isEqualTo("view");
        assertThat(acessos.get("estoque")).isNull();
        assertThat(acessos.get("financeiro")).isNull();
    }
}