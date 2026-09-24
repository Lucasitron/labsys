package com.fablab.vendas.service;

import com.fablab.vendas.dto.ClienteDtos.BulkTagRequest;
import com.fablab.vendas.dto.ClienteDtos.ClienteDetalheResponse;
import com.fablab.vendas.dto.ClienteDtos.ClientePaginaResponse;
import com.fablab.vendas.dto.ClienteDtos.ClienteRequest;
import com.fablab.vendas.dto.ClienteDtos.ClienteResponse;
import com.fablab.vendas.dto.ClienteDtos.Paginacao;
import com.fablab.vendas.dto.ClienteDtos.TagResumo;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.ClienteTag;
import com.fablab.vendas.entity.TagCliente;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ForbiddenException;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.ClienteTagRepository;
import com.fablab.vendas.repository.EncomendaRepository;
import com.fablab.vendas.repository.InteracaoClienteRepository;
import com.fablab.vendas.repository.OrcamentoRepository;
import com.fablab.vendas.repository.TagClienteRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de clientes PF/PJ com validação de CPF/CNPJ, máscara LGPD (D-7) e
 * enforcement criador+Admin (D-3).
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteTagRepository clienteTagRepository;
    private final TagClienteRepository tagRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final EncomendaRepository encomendaRepository;
    private final InteracaoClienteRepository interacaoRepository;
    private final DocumentoUtil documentoUtil;
    private final PermissaoUtil permissaoUtil;

    public ClienteService(ClienteRepository clienteRepository,
                          ClienteTagRepository clienteTagRepository,
                          TagClienteRepository tagRepository,
                          OrcamentoRepository orcamentoRepository,
                          EncomendaRepository encomendaRepository,
                          InteracaoClienteRepository interacaoRepository,
                          DocumentoUtil documentoUtil,
                          PermissaoUtil permissaoUtil) {
        this.clienteRepository = clienteRepository;
        this.clienteTagRepository = clienteTagRepository;
        this.tagRepository = tagRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.encomendaRepository = encomendaRepository;
        this.interacaoRepository = interacaoRepository;
        this.documentoUtil = documentoUtil;
        this.permissaoUtil = permissaoUtil;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request, VendasPrincipal principal) {
        String digitos = documentoUtil.somenteDigitos(request.cpfCnpj());
        if (!documentoUtil.valido(request.cpfCnpj())) {
            throw new IllegalArgumentException("CPF ou CNPJ inválido");
        }
        if (clienteRepository.existsByCpfCnpj(digitos)) {
            throw new ConflitoException("CPF ou CNPJ já cadastrado");
        }
        Cliente cliente = new Cliente();
        cliente.setTipoPessoa(TipoPessoa.valueOf(request.tipoPessoa()));
        cliente.setNomeRazaoSocial(request.nomeRazaoSocial());
        cliente.setCpfCnpj(digitos);
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        cliente.setDataCadastro(LocalDate.now());
        cliente.setCriadoPor(principal.idPessoa());
        return paraResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClientePaginaResponse listar(String termo, String tipo, List<Long> tags, int page, int pageSize) {
        PageRequest paginacao = PageRequest.of(Math.max(page - 1, 0), pageSize, Sort.by("nomeRazaoSocial"));
        Page<Cliente> pagina;
        if (termo != null && !termo.isBlank()) {
            pagina = clienteRepository.buscar(termo.trim(), paginacao);
        } else {
            pagina = clienteRepository.findAll(paginacao);
        }
        List<Cliente> filtrados = new ArrayList<>(pagina.getContent());
        if (tipo != null && !tipo.isBlank()) {
            TipoPessoa tipoPessoa = TipoPessoa.valueOf(tipo.toUpperCase());
            filtrados.removeIf(c -> c.getTipoPessoa() != tipoPessoa);
        }
        if (tags != null && !tags.isEmpty()) {
            List<Long> comTags = clienteRepository.buscarPorTags(tags, tags.size())
                    .stream().map(Cliente::getId).toList();
            filtrados.removeIf(c -> !comTags.contains(c.getId()));
        }
        List<ClienteResponse> respostas = filtrados.stream().map(this::paraResponse).toList();
        long totalPages = pagina.getTotalPages() == 0 ? 1 : pagina.getTotalPages();
        Map<String, List<Map<String, Object>>> filters = new LinkedHashMap<>();
        filters.put("tipos", List.of(
                Map.of("id", "PF", "label", "Pessoa física",
                        "count", clienteRepository.countByTipoPessoa(TipoPessoa.PF)),
                Map.of("id", "PJ", "label", "Pessoa jurídica",
                        "count", clienteRepository.countByTipoPessoa(TipoPessoa.PJ))));
        List<Map<String, Object>> tagsFiltro = tagRepository.findAll().stream()
                .sorted(Comparator.comparing(TagCliente::getNome))
                .map(t -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", t.getId());
                    item.put("nome", t.getNome());
                    item.put("count", clienteTagRepository.countByIdTag(t.getId()));
                    return item;
                }).toList();
        filters.put("tags", tagsFiltro);
        return new ClientePaginaResponse(respostas,
                new Paginacao(page, pageSize, pagina.getTotalElements(), (int) totalPages), filters);
    }

    @Transactional(readOnly = true)
    public ClienteDetalheResponse detalhar(Long id) {
        Cliente cliente = buscar(id);
        Map<String, Object> indicadores = new LinkedHashMap<>();
        indicadores.put("orcamentosEmAberto",
                orcamentoRepository.findByIdCliente(id).stream()
                        .filter(o -> o.getStatus().name().equals("Pendente")
                                || o.getStatus().name().equals("Ajuste")).count());
        indicadores.put("encomendasEmProducao",
                encomendaRepository.findByIdCliente(id).stream()
                        .filter(e -> !e.getStatusKanban().name().equals("Entregue")).count());
        indicadores.put("ultimaInteracao", interacaoRepository.findByIdClienteOrderByDataInteracaoDesc(id)
                .stream().findFirst().map(i -> i.getDataInteracao().toString()).orElse(null));
        ClienteResponse base = paraResponse(cliente);
        return new ClienteDetalheResponse(base.id(), base.tipoPessoa(), base.nomeRazaoSocial(),
                base.documento(), base.email(), base.telefone(), base.endereco(), base.dataCadastro(),
                base.tags(), base.criadoPor(), indicadores);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request, VendasPrincipal principal) {
        Cliente cliente = buscar(id);
        permissaoUtil.exigirCriadorOuAdmin(cliente.getCriadoPor(), principal);
        String digitos = documentoUtil.somenteDigitos(request.cpfCnpj());
        if (!documentoUtil.valido(request.cpfCnpj())) {
            throw new IllegalArgumentException("CPF ou CNPJ inválido");
        }
        clienteRepository.findByCpfCnpj(digitos).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new ConflitoException("CPF ou CNPJ já cadastrado");
            }
        });
        cliente.setTipoPessoa(TipoPessoa.valueOf(request.tipoPessoa()));
        cliente.setNomeRazaoSocial(request.nomeRazaoSocial());
        cliente.setCpfCnpj(digitos);
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        return paraResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void excluir(Long id, VendasPrincipal principal) {
        Cliente cliente = buscar(id);
        permissaoUtil.exigirCriadorOuAdmin(cliente.getCriadoPor(), principal);
        if (encomendaRepository.existsByIdCliente(id)) {
            throw new ConflitoException("Cliente possui encomendas vinculadas e não pode ser removido");
        }
        clienteTagRepository.findByIdCliente(id).forEach(clienteTagRepository::delete);
        clienteRepository.delete(cliente);
    }

    @Transactional
    public void vincularTag(Long idCliente, Long idTag, VendasPrincipal principal) {
        Cliente cliente = buscar(idCliente);
        permissaoUtil.exigirCriadorOuAdmin(cliente.getCriadoPor(), principal);
        tagRepository.findById(idTag)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada"));
        if (!clienteTagRepository.existsByIdClienteAndIdTag(idCliente, idTag)) {
            ClienteTag vinculo = new ClienteTag();
            vinculo.setIdCliente(idCliente);
            vinculo.setIdTag(idTag);
            clienteTagRepository.save(vinculo);
        }
    }

    @Transactional
    public void desvincularTag(Long idCliente, Long idTag, VendasPrincipal principal) {
        Cliente cliente = buscar(idCliente);
        permissaoUtil.exigirCriadorOuAdmin(cliente.getCriadoPor(), principal);
        clienteTagRepository.deleteByIdClienteAndIdTag(idCliente, idTag);
    }

    /** D-2 confirmado: marcação de tag em lote (sem duplicidade). */
    @Transactional
    public Map<String, Object> bulkTag(BulkTagRequest request, VendasPrincipal principal) {
        if (request.clienteIds() == null || request.clienteIds().isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um cliente");
        }
        tagRepository.findById(request.tagId())
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada"));
        if (!principal.isAdmin()) {
            throw new ForbiddenException("Apenas Admin pode marcar tags em lote");
        }
        int vinculados = 0;
        for (Long idCliente : request.clienteIds()) {
            Cliente cliente = buscar(idCliente);
            if (!clienteTagRepository.existsByIdClienteAndIdTag(idCliente, request.tagId())) {
                ClienteTag vinculo = new ClienteTag();
                vinculo.setIdCliente(cliente.getId());
                vinculo.setIdTag(request.tagId());
                clienteTagRepository.save(vinculo);
                vinculados++;
            }
        }
        return Map.of("vinculados", vinculados, "tagId", request.tagId());
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
    }

    private ClienteResponse paraResponse(Cliente cliente) {
        List<TagResumo> tags = clienteTagRepository.findByIdCliente(cliente.getId()).stream()
                .map(v -> tagRepository.findById(v.getIdTag()).orElse(null))
                .filter(t -> t != null)
                .map(t -> new TagResumo(t.getId(), t.getNome(), t.getCor()))
                .toList();
        return new ClienteResponse(cliente.getId(), cliente.getTipoPessoa().name(),
                cliente.getNomeRazaoSocial(), documentoUtil.mascarar(cliente.getCpfCnpj()),
                cliente.getEmail(), cliente.getTelefone(), cliente.getEndereco(),
                cliente.getDataCadastro(), tags, cliente.getCriadoPor());
    }
}
