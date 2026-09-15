package com.fablab.vendas.service;

import com.fablab.vendas.dto.InteracaoRequest;
import com.fablab.vendas.dto.InteracaoResponse;
import com.fablab.vendas.entity.Cliente;
import com.fablab.vendas.entity.InteracaoCliente;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.ClienteRepository;
import com.fablab.vendas.repository.InteracaoClienteRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRM interno: registro e consulta de interações com clientes.
 */
@Service
public class InteracaoService {

    private final InteracaoClienteRepository interacaoRepository;
    private final ClienteRepository clienteRepository;

    public InteracaoService(InteracaoClienteRepository interacaoRepository,
                            ClienteRepository clienteRepository) {
        this.interacaoRepository = interacaoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public InteracaoResponse registrar(InteracaoRequest request) {
        Cliente cliente = clienteRepository.findById(request.idCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.idCliente()));

        InteracaoCliente interacao = new InteracaoCliente();
        interacao.setCliente(cliente);
        interacao.setDataInteracao(
                request.dataInteracao() != null ? request.dataInteracao() : LocalDateTime.now());
        interacao.setTipo(request.tipo());
        interacao.setDescricao(request.descricao());
        interacao.setIdUsuario(request.idUsuario());
        return InteracaoResponse.of(interacaoRepository.save(interacao));
    }

    @Transactional(readOnly = true)
    public List<InteracaoResponse> listarPorCliente(Long idCliente) {
        clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + idCliente));
        return interacaoRepository.findByCliente_IdOrderByDataInteracaoDesc(idCliente)
                .stream().map(InteracaoResponse::of).toList();
    }
}