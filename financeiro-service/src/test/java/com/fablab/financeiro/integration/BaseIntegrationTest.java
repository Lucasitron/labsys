package com.fablab.financeiro.integration;

import com.fablab.financeiro.TokenHelper;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.NivelAcesso;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import com.fablab.financeiro.repository.CustoEncomendaRepository;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import com.fablab.financeiro.repository.LancamentoFinanceiroRepository;
import com.fablab.financeiro.repository.ParametroOverheadRepository;
import com.fablab.financeiro.repository.SolicitacaoCompraRepository;
import com.fablab.financeiro.repository.ValorHoraNivelRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base para testes de integração: contexto completo com banco H2 e RabbitMQ
 * substituído por mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext
@MockBean(RabbitTemplate.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CategoriaFinanceiraRepository categoriaRepository;
    @Autowired
    protected LancamentoFinanceiroRepository lancamentoRepository;
    @Autowired
    protected DoacaoRecursoRepository doacaoRepository;
    @Autowired
    protected ValorHoraNivelRepository valorHoraRepository;
    @Autowired
    protected ParametroOverheadRepository parametroRepository;
    @Autowired
    protected FechamentoEncomendaRepository fechamentoRepository;
    @Autowired
    protected CustoEncomendaRepository custoRepository;
    @Autowired
    protected SolicitacaoCompraRepository solicitacaoRepository;
    @Autowired
    protected HorasEncomendaRepository horasRepository;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected String token(Long idPessoa, NivelAcesso nivel) {
        return TokenHelper.token(idPessoa, nivel);
    }

    protected CategoriaFinanceira seedCategoria(String nome, TipoCategoriaFinanceira tipo) {
        CategoriaFinanceira categoria = new CategoriaFinanceira();
        categoria.setNome(nome);
        categoria.setTipo(tipo);
        return categoriaRepository.save(categoria);
    }
}