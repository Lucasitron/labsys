package com.fablab.estoque.integration;

import com.fablab.estoque.TokenHelper;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.Localizacao;
import com.fablab.estoque.entity.NivelAcesso;
import com.fablab.estoque.repository.EntradaEstoqueRepository;
import com.fablab.estoque.repository.EmprestimoRepository;
import com.fablab.estoque.repository.FornecedorRepository;
import com.fablab.estoque.repository.ItemBomRepository;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.ListaMateriaisRepository;
import com.fablab.estoque.repository.LocalizacaoRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import com.fablab.estoque.service.ItemService;
import java.math.BigDecimal;
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
    protected ItemRepository itemRepository;
    @Autowired
    protected LocalizacaoRepository localizacaoRepository;
    @Autowired
    protected FornecedorRepository fornecedorRepository;
    @Autowired
    protected EntradaEstoqueRepository entradaEstoqueRepository;
    @Autowired
    protected SaidaEstoqueRepository saidaEstoqueRepository;
    @Autowired
    protected EmprestimoRepository emprestimoRepository;
    @Autowired
    protected ListaMateriaisRepository bomRepository;
    @Autowired
    protected ItemBomRepository itemBomRepository;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected RabbitTemplate rabbitTemplate;

    protected Localizacao seedLocalizacao(String armario, String prateleira, String caixa) {
        Localizacao loc = new Localizacao();
        loc.setArmario(armario);
        loc.setPrateleira(prateleira);
        loc.setCaixa(caixa);
        return localizacaoRepository.save(loc);
    }

    protected Item seedItem(String nome, Categoria categoria, BigDecimal qtdAtual, BigDecimal estoqueMin) {
        return seedItem(nome, categoria, qtdAtual, estoqueMin, null);
    }

    protected Item seedItem(String nome, Categoria categoria, BigDecimal qtdAtual, BigDecimal estoqueMin,
                            Localizacao localizacao) {
        Item item = new Item();
        item.setNome(nome);
        item.setCategoria(categoria);
        item.setUnidadeMedida("un");
        item.setQuantidadeAtual(qtdAtual);
        item.setEstoqueMinimo(estoqueMin);
        item.setLocalizacao(localizacao);
        return itemRepository.save(item);
    }

    protected String token(Long idPessoa, NivelAcesso nivel) {
        return TokenHelper.token(idPessoa, nivel);
    }
}