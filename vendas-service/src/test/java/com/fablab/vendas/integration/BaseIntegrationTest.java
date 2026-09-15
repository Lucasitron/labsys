package com.fablab.vendas.integration;

import com.fablab.vendas.TokenHelper;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.ItemOrcamento;
import com.fablab.vendas.entity.NivelAcesso;
import com.fablab.vendas.entity.Orcamento;
import com.fablab.vendas.entity.StatusOrcamento;
import com.fablab.vendas.entity.TagCliente;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import com.fablab.vendas.repository.InteracaoClienteRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import com.fablab.vendas.repository.RegistroMarketplaceRepository;
import com.fablab.vendas.repository.TagClienteRepository;
import com.fablab.vendas.repository.TarefaMarketingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    protected ClienteRepository clienteRepository;
    @Autowired
    protected TagClienteRepository tagClienteRepository;
    @Autowired
    protected OrcamentoRepository orcamentoRepository;
    @Autowired
    protected EncomendaRepository encomendaRepository;
    @Autowired
    protected HistoricoStatusEncomendaRepository historicoRepository;
    @Autowired
    protected RegistroMarketplaceRepository registroMarketplaceRepository;
    @Autowired
    protected InteracaoClienteRepository interacaoRepository;
    @Autowired
    protected TarefaMarketingRepository tarefaRepository;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected String token(Long idPessoa, NivelAcesso nivel) {
        return TokenHelper.token(idPessoa, nivel);
    }

    protected Cliente seedCliente(String nome) {
        return seedCliente(nome, "52998224725", TipoPessoa.PF);
    }

    protected Cliente seedCliente(String nome, String cpfCnpj, TipoPessoa tipo) {
        Cliente cliente = new Cliente();
        cliente.setTipoPessoa(tipo);
        cliente.setNomeRazaoSocial(nome);
        cliente.setCpfCnpj(cpfCnpj);
        cliente.setDataCadastro(LocalDate.now());
        return clienteRepository.save(cliente);
    }

    protected TagCliente seedTag(String nome, String cor) {
        TagCliente tag = new TagCliente();
        tag.setNome(nome);
        tag.setCor(cor);
        return tagClienteRepository.save(tag);
    }

    protected Orcamento seedOrcamento(Long idCliente, StatusOrcamento status, int quantidadeItens) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new IllegalStateException("Cliente não encontrado"));
        Orcamento orcamento = new Orcamento();
        orcamento.setCliente(cliente);
        orcamento.setDataCriacao(LocalDate.now());
        orcamento.setValidade(LocalDate.now().plusDays(15));
        orcamento.setStatus(status);
        for (int i = 0; i < quantidadeItens; i++) {
            ItemOrcamento item = new ItemOrcamento();
            item.setDescricao("Item " + (i + 1));
            item.setQuantidade(BigDecimal.ONE);
            item.setValorUnitario(BigDecimal.TEN);
            orcamento.adicionarItem(item);
        }
        orcamento.recalcularValorTotal();
        return orcamentoRepository.save(orcamento);
    }
}