package com.fablab.rh.integration;

import com.fablab.rh.TokenHelper;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.Tutor;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.AvaliacaoTreinamentoRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HistoricoNivelRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.ProcessoSeletivoRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import com.fablab.rh.repository.TreinamentoRepository;
import com.fablab.rh.repository.TutorRepository;
import com.fablab.rh.service.PontoService;
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
    protected PessoaRepository pessoaRepository;
    @Autowired
    protected FuncionarioRepository funcionarioRepository;
    @Autowired
    protected TutorRepository tutorRepository;
    @Autowired
    protected RegistroPontoDiarioRepository pontoRepository;
    @Autowired
    protected ApontamentoHorasRepository apontamentoRepository;
    @Autowired
    protected ProcessoSeletivoRepository processoRepository;
    @Autowired
    protected TreinamentoRepository treinamentoRepository;
    @Autowired
    protected AvaliacaoTreinamentoRepository avaliacaoRepository;
    @Autowired
    protected HistoricoNivelRepository historicoRepository;
    @Autowired
    protected PontoService pontoService;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected Pessoa seedPessoa(String nome, String matricula) {
        return seedPessoa(nome, matricula, PessoaStatus.ATIVO);
    }

    protected Pessoa seedPessoa(String nome, String matricula, PessoaStatus status) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNomeCompleto(nome);
        pessoa.setMatricula(matricula);
        pessoa.setStatus(status);
        return pessoaRepository.save(pessoa);
    }

    protected Funcionario seedFuncionario(Pessoa pessoa, NivelAcesso nivel, String departamento) {
        Funcionario funcionario = new Funcionario();
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(nivel);
        funcionario.setDepartamento(departamento);
        return funcionarioRepository.save(funcionario);
    }

    protected Tutor seedTutor(Funcionario funcionario) {
        Tutor tutor = new Tutor();
        tutor.setFuncionario(funcionario);
        tutor.setQualificacao(2);
        return tutorRepository.save(tutor);
    }

    protected String token(Pessoa pessoa, NivelAcesso nivel) {
        return TokenHelper.token(pessoa.getId(), nivel);
    }

    protected String token(Long idPessoa, NivelAcesso nivel, Long idFuncionario) {
        return TokenHelper.token(idPessoa, nivel, idFuncionario);
    }
}