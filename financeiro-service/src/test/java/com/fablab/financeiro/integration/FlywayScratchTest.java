package com.fablab.financeiro.integration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

/** Scratch: valida V1..V9 no H2 (modo PostgreSQL) + ddl-auto=validate. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fablabfinanceiroflyway;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class FlywayScratchTest {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationsAplicamEEntidadesValidam() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer tabelas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN "
                + "('categoria_financeira','lancamento_financeiro','doacao_recurso','valor_hora_nivel',"
                + "'parametro_overhead','fechamento_encomenda','custo_encomenda','solicitacao_compra',"
                + "'horas_encomenda')",
                Integer.class);
        assertThat(tabelas).isEqualTo(9);
    }
}
