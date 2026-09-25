package com.fablab.producao.integration;

import com.fablab.producao.TokenHelper;
import com.fablab.producao.entity.NivelAcesso;
import com.fablab.producao.repository.AdvertenciaMembroRepository;
import com.fablab.producao.repository.AuditoriaProjetoMesaRepository;
import com.fablab.producao.repository.ConsumoEncomendaRepository;
import com.fablab.producao.repository.EncomendaKanbanRepository;
import com.fablab.producao.repository.HistoricoKanbanRepository;
import com.fablab.producao.repository.HistoricoUsoMaquinaRepository;
import com.fablab.producao.repository.Inspecao5SRepository;
import com.fablab.producao.repository.MaquinaRepository;
import com.fablab.producao.repository.Parametro5SRepository;
import com.fablab.producao.repository.ProjetoMesaRepository;
import com.fablab.producao.repository.ProjetoRepository;
import com.fablab.producao.repository.SetorChecklistRepository;
import com.fablab.producao.repository.SetorMaterialRepository;
import com.fablab.producao.repository.SetorRepository;
import com.fablab.producao.repository.SetorResponsavelRepository;
import com.fablab.producao.repository.SetorSinalizacaoRepository;
import com.fablab.producao.repository.TarefaRepository;
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
    protected ProjetoRepository projetoRepository;
    @Autowired
    protected TarefaRepository tarefaRepository;
    @Autowired
    protected EncomendaKanbanRepository kanbanRepository;
    @Autowired
    protected HistoricoKanbanRepository historicoKanbanRepository;
    @Autowired
    protected MaquinaRepository maquinaRepository;
    @Autowired
    protected HistoricoUsoMaquinaRepository usoMaquinaRepository;
    @Autowired
    protected SetorRepository setorRepository;
    @Autowired
    protected SetorMaterialRepository materialRepository;
    @Autowired
    protected SetorSinalizacaoRepository sinalizacaoRepository;
    @Autowired
    protected SetorChecklistRepository checklistRepository;
    @Autowired
    protected SetorResponsavelRepository responsavelRepository;
    @Autowired
    protected Inspecao5SRepository inspecaoRepository;
    @Autowired
    protected AdvertenciaMembroRepository advertenciaRepository;
    @Autowired
    protected ProjetoMesaRepository projetoMesaRepository;
    @Autowired
    protected AuditoriaProjetoMesaRepository auditoriaRepository;
    @Autowired
    protected Parametro5SRepository parametroRepository;
    @Autowired
    protected ConsumoEncomendaRepository consumoRepository;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected String token(Long idPessoa, NivelAcesso nivel) {
        return TokenHelper.token(idPessoa, nivel);
    }

    protected String bearer(Long idPessoa, NivelAcesso nivel) {
        return "Bearer " + token(idPessoa, nivel);
    }
}