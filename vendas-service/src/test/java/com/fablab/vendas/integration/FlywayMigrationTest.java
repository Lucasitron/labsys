package com.fablab.vendas.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Migrations V1..V11 aplicam em banco limpo (H2 em modo PostgreSQL). */
class FlywayMigrationTest {

    @Test
    void migrationsAplicamEmBancoLimpo() throws Exception {
        String url = "jdbc:h2:mem:flywaycheck;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        Set<String> tabelas = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(url, "sa", "");
             ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tabelas.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        for (String esperada : new String[]{"cliente", "tag_cliente", "cliente_tag", "orcamento",
                "item_orcamento", "encomenda", "historico_status_encomenda", "registro_marketplace",
                "interacao_cliente", "tarefa_marketing", "solicitacao_edicao"}) {
            assertTrue(tabelas.contains(esperada), "Tabela ausente: " + esperada);
        }
    }
}
