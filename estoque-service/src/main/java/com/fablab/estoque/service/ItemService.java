package com.fablab.estoque.service;

import com.fablab.estoque.dto.ItemConsumido;
import com.fablab.estoque.dto.ItemRequest;
import com.fablab.estoque.dto.ItemResponse;
import com.fablab.estoque.dto.SaidaResponse;
import com.fablab.estoque.entity.Categoria;
import com.fablab.estoque.entity.Item;
import com.fablab.estoque.entity.Localizacao;
import com.fablab.estoque.entity.SaidaEstoque;
import com.fablab.estoque.entity.TipoSaida;
import com.fablab.estoque.exception.ResourceNotFoundException;
import com.fablab.estoque.exception.SaldoInsuficienteException;
import com.fablab.estoque.mapper.ItemMapper;
import com.fablab.estoque.mapper.SaidaMapper;
import com.fablab.estoque.rabbit.EstoqueEventPublisher;
import com.fablab.estoque.repository.ItemRepository;
import com.fablab.estoque.repository.LocalizacaoRepository;
import com.fablab.estoque.repository.SaidaEstoqueRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de itens de inventário, consumo via BOM e import/export CSV.
 */
@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemRepository itemRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final SaidaEstoqueRepository saidaEstoqueRepository;
    private final EstoqueEventPublisher eventPublisher;

    public ItemService(ItemRepository itemRepository,
                       LocalizacaoRepository localizacaoRepository,
                       SaidaEstoqueRepository saidaEstoqueRepository,
                       EstoqueEventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.localizacaoRepository = localizacaoRepository;
        this.saidaEstoqueRepository = saidaEstoqueRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ItemResponse criar(ItemRequest request) {
        Localizacao localizacao = resolverLocalizacao(request.idLocalizacao());
        Item item = ItemMapper.toEntity(request, localizacao);
        item = itemRepository.save(item);
        verificarEstoqueBaixo(item);
        return ItemMapper.toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> listar(Categoria categoria, Long idLocalizacao, Boolean baixo) {
        List<Item> itens = itemRepository.findAll();
        if (categoria != null) {
            itens = itens.stream().filter(i -> i.getCategoria() == categoria).toList();
        }
        if (idLocalizacao != null) {
            itens = itens.stream()
                    .filter(i -> i.getLocalizacao() != null && i.getLocalizacao().getId().equals(idLocalizacao))
                    .toList();
        }
        if (Boolean.TRUE.equals(baixo)) {
            itens = itens.stream()
                    .filter(i -> i.getQuantidadeAtual().compareTo(i.getEstoqueMinimo()) <= 0)
                    .toList();
        }
        return itens.stream().map(ItemMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse buscar(Long id) {
        return ItemMapper.toResponse(obter(id));
    }

    @Transactional
    public ItemResponse atualizar(Long id, ItemRequest request) {
        Item item = obter(id);
        ItemMapper.update(item, request, resolverLocalizacao(request.idLocalizacao()));
        return ItemMapper.toResponse(itemRepository.save(item));
    }

    /**
     * Baixa automática de estoque pelos itens efetivamente consumidos (fluxo de
     * produção concluída). A baixa é idempotente por {@code idReferencia}.
     */
    @Transactional
    public List<SaidaResponse> baixarPorConsumo(List<ItemConsumido> itensConsumidos, Long idReferencia) {
        if (idReferencia != null
                && saidaEstoqueRepository.existsByTipoSaidaAndIdReferencia(TipoSaida.CONSUMO, idReferencia)) {
            log.info("Baixa por consumo já registrada para a referência {}; ignorando evento duplicado", idReferencia);
            return List.of();
        }
        List<SaidaResponse> saidas = new ArrayList<>();
        for (ItemConsumido consumo : itensConsumidos) {
            saidas.add(registrarConsumo(consumo.idItem(), consumo.quantidadeConsumida(), idReferencia));
        }
        return saidas;
    }

    /** Registra o consumo efetivo de um item (saída {@code tipo_saida = CONSUMO}). */
    @Transactional
    public SaidaResponse registrarConsumo(Long idItem, BigDecimal quantidade, Long idReferencia) {
        Item item = itemRepository.findByIdForUpdate(idItem)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + idItem));
        subtrair(item, quantidade);

        SaidaEstoque saida = novaSaida(item, quantidade, TipoSaida.CONSUMO, idReferencia, "Consumo registrado via BOM");
        saida = saidaEstoqueRepository.save(saida);
        verificarEstoqueBaixo(item);
        return SaidaMapper.toResponse(saida);
    }

    /**
     * Gera a listagem de itens em CSV (export).
     *
     * <p>Formato separado por ponto e vírgula, com cabeçalho em PT-BR:
     * {@code id_item;nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;localizacao}.</p>
     */
    @Transactional(readOnly = true)
    public String exportarCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("id_item;nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;localizacao\n");
        itemRepository.findAll().forEach(item -> {
            String localizacao = item.getLocalizacao() == null ? ""
                    : item.getLocalizacao().getArmario() + "/" + item.getLocalizacao().getPrateleira()
                    + "/" + item.getLocalizacao().getCaixa();
            sb.append(item.getId()).append(';')
                    .append(csv(item.getNome())).append(';')
                    .append(csv(item.getDescricao())).append(';')
                    .append(item.getCategoria()).append(';')
                    .append(csv(item.getUnidadeMedida())).append(';')
                    .append(item.getQuantidadeAtual()).append(';')
                    .append(item.getEstoqueMinimo()).append(';')
                    .append(csv(localizacao))
                    .append('\n');
        });
        return sb.toString();
    }

    /**
     * Importa itens a partir do CSV enviado.
     *
     * <p>Formato separado por ponto e vírgula, sem o cabeçalho:
     * {@code nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;id_localizacao(optional)}.</p>
     */
    @Transactional
    public List<ItemResponse> importarCsv(String conteudo) {
        List<ItemResponse> importados = new ArrayList<>();
        String[] linhas = conteudo.split("\n");
        for (String linha : linhas) {
            String linhaClean = linha.trim();
            if (linhaClean.isEmpty()) {
                continue;
            }
            String[] colunas = linhaClean.split(";");
            if (colunas.length < 6) {
                throw new IllegalArgumentException(
                        "Linha inválida no CSV de importação (esperado: nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo): "
                                + linhaClean);
            }
            String nome = colunas[0].trim();
            String descricao = colunas[1].trim();
            Categoria categoria = Categoria.valueOf(colunas[2].trim().toUpperCase());
            String unidadeMedida = colunas[3].trim();
            BigDecimal quantidadeAtual = new BigDecimal(colunas[4].trim().replace(',', '.'));
            BigDecimal estoqueMinimo = new BigDecimal(colunas[5].trim().replace(',', '.'));
            Long idLocalizacao = colunas.length >= 7 && !colunas[6].trim().isEmpty()
                    ? Long.valueOf(colunas[6].trim()) : null;

            ItemRequest request = new ItemRequest(nome, descricao, categoria, unidadeMedida,
                    quantidadeAtual, estoqueMinimo, idLocalizacao);
            importados.add(criar(request));
        }
        return importados;
    }

    /** Publica o evento de estoque baixo quando {@code quantidade_atual <= estoque_minimo}. */
    public void verificarEstoqueBaixo(Item item) {
        if (item.getQuantidadeAtual().compareTo(item.getEstoqueMinimo()) <= 0) {
            eventPublisher.publishEstoqueBaixo(item);
        }
    }

    private Item obter(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + id));
    }

    private Localizacao resolverLocalizacao(Long idLocalizacao) {
        if (idLocalizacao == null) {
            return null;
        }
        return localizacaoRepository.findById(idLocalizacao)
                .orElseThrow(() -> new ResourceNotFoundException("Localização não encontrada: " + idLocalizacao));
    }

    private void subtrair(Item item, BigDecimal quantidade) {
        BigDecimal novoSaldo = item.getQuantidadeAtual().subtract(quantidade);
        if (novoSaldo.signum() < 0) {
            throw new SaldoInsuficienteException(
                    "Estoque insuficiente para o item " + item.getNome() + ": saldo atual "
                            + item.getQuantidadeAtual() + ", necessário " + quantidade);
        }
        item.setQuantidadeAtual(novoSaldo);
    }

    private SaidaEstoque novaSaida(Item item, BigDecimal quantidade, TipoSaida tipoSaida,
                                   Long idReferencia, String observacao) {
        SaidaEstoque saida = new SaidaEstoque();
        saida.setItem(item);
        saida.setQuantidade(quantidade);
        saida.setTipoSaida(tipoSaida);
        saida.setIdReferencia(idReferencia);
        saida.setDataSaida(LocalDateTime.now());
        saida.setObservacao(observacao);
        return saida;
    }

    private String csv(String valor) {
        return valor == null ? "" : valor.replace(";", ",");
    }
}