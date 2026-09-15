package com.fablab.vendas.service;

import com.fablab.vendas.dto.ClienteRequest;
import com.fablab.vendas.dto.ClienteResponse;
import com.fablab.vendas.dto.TagClienteRequest;
import com.fablab.vendas.dto.TagClienteResponse;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.TagCliente;
import com.fablab.vendas.entity.TipoPessoa;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.ClienteTagRepository;
import com.fablab.vendas.repository.TagClienteRepository;
import com.fablab.vendas.util.DocumentoValidator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de clientes (PF e PJ), tags e vinculação cliente-tag.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TagClienteRepository tagClienteRepository;
    private final ClienteTagRepository clienteTagRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          TagClienteRepository tagClienteRepository,
                          ClienteTagRepository clienteTagRepository) {
        this.clienteRepository = clienteRepository;
        this.tagClienteRepository = tagClienteRepository;
        this.clienteTagRepository = clienteTagRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        validarDocumento(request.tipoPessoa(), request.cpfCnpj());
        if (clienteRepository.existsByCpfCnpj(request.cpfCnpj())) {
            throw new IllegalArgumentException("Já existe cliente cadastrado com o documento informado");
        }

        Cliente cliente = new Cliente();
        cliente.setTipoPessoa(request.tipoPessoa());
        cliente.setNomeRazaoSocial(request.nomeRazaoSocial());
        cliente.setCpfCnpj(request.cpfCnpj());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        cliente.setDataCadastro(java.time.LocalDate.now());
        cliente = clienteRepository.save(cliente);

        if (request.tags() != null) {
            for (Long idTag : request.tags()) {
                TagCliente tag = tagClienteRepository.findById(idTag)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada: " + idTag));
                cliente.adicionarTag(tag);
            }
        }

        return ClienteResponse.of(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(TipoPessoa tipoPessoa, Long idTag, String nome) {
        List<Cliente> clientes;

        if (nome != null && !nome.isBlank()) {
            clientes = clienteRepository.findByNomeRazaoSocialContainingIgnoreCase(nome);
        } else if (tipoPessoa != null) {
            clientes = clienteRepository.findByTipoPessoa(tipoPessoa);
        } else {
            clientes = clienteRepository.findAll();
        }

        if (idTag != null) {
            clientes = clientes.stream()
                    .filter(c -> c.getTags().stream()
                            .anyMatch(ct -> ct.getTag().getId().equals(idTag)))
                    .toList();
        }

        return clientes.stream().map(ClienteResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.of(obter(id));
    }

    @Transactional
    public ClienteResponse adicionarTag(Long idCliente, Long idTag) {
        Cliente cliente = obter(idCliente);
        TagCliente tag = tagClienteRepository.findById(idTag)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada: " + idTag));

        if (clienteTagRepository.existsByCliente_IdAndTag_Id(idCliente, idTag)) {
            throw new IllegalArgumentException("Cliente já possui a tag informada");
        }

        cliente.adicionarTag(tag);
        return ClienteResponse.of(cliente);
    }

    @Transactional
    public TagClienteResponse criarTag(TagClienteRequest request) {
        tagClienteRepository.findByNomeIgnoreCase(request.nome()).ifPresent(t -> {
            throw new IllegalArgumentException("Já existe tag com o nome informado");
        });
        TagCliente tag = new TagCliente();
        tag.setNome(request.nome());
        tag.setCor(request.cor());
        return TagClienteResponse.of(tagClienteRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<TagClienteResponse> listarTags() {
        return tagClienteRepository.findAll().stream().map(TagClienteResponse::of).toList();
    }

    private Cliente obter(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }

    private void validarDocumento(TipoPessoa tipo, String documento) {
        if (tipo == TipoPessoa.PF && !DocumentoValidator.isCpfValido(documento)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (tipo == TipoPessoa.PJ && !DocumentoValidator.isCnpjValido(documento)) {
            throw new IllegalArgumentException("CNPJ inválido");
        }
    }
}