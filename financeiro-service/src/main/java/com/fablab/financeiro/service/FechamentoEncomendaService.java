package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.EncomendaCriadaEvent;
import com.fablab.financeiro.dto.FechamentoEncomendaRequest;
import com.fablab.financeiro.dto.FechamentoEncomendaResponse;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.StatusFechamentoEncomenda;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fechamento de encomendas e acúmulo de horas validadas para o custeio.
 */
@Service
public class FechamentoEncomendaService {

    private final FechamentoEncomendaRepository fechamentoRepository;
    private final HorasEncomendaRepository horasRepository;

    public FechamentoEncomendaService(FechamentoEncomendaRepository fechamentoRepository,
                                      HorasEncomendaRepository horasRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.horasRepository = horasRepository;
    }

    @Transactional
    public FechamentoEncomendaResponse criar(FechamentoEncomendaRequest request) {
        if (fechamentoRepository.existsByIdEncomenda(request.idEncomenda())) {
            throw new IllegalArgumentException("Encomenda já possui fechamento: " + request.idEncomenda());
        }
        FechamentoEncomenda fechamento = new FechamentoEncomenda();
        fechamento.setIdEncomenda(request.idEncomenda());
        fechamento.setHorasEstimadas(request.horasEstimadas());
        fechamento.setValorFechado(request.valorFechado());
        fechamento.setDataFechamento(request.dataFechamento());
        fechamento.setStatus(StatusFechamentoEncomenda.ABERTA);
        fechamento.setHorasValidadas(BigDecimal.ZERO);
        return FechamentoEncomendaResponse.of(fechamentoRepository.save(fechamento));
    }

    /**
     * Cria o fechamento em {@code ABERTA} a partir da encomenda criada no
     * Vendas &amp; CRM. Idempotente: se já existir, apenas retorna o atual.
     */
    @Transactional
    public FechamentoEncomenda criarDoEvento(EncomendaCriadaEvent event) {
        if (fechamentoRepository.existsByIdEncomenda(event.idEncomenda())) {
            return obter(event.idEncomenda());
        }
        FechamentoEncomenda fechamento = new FechamentoEncomenda();
        fechamento.setIdEncomenda(event.idEncomenda());
        fechamento.setValorFechado(event.valorFinal() != null ? event.valorFinal() : BigDecimal.ZERO);
        fechamento.setDataFechamento(event.dataCriacao() != null ? event.dataCriacao() : LocalDate.now());
        fechamento.setStatus(StatusFechamentoEncomenda.ABERTA);
        fechamento.setHorasValidadas(BigDecimal.ZERO);
        return fechamentoRepository.save(fechamento);
    }

    @Transactional(readOnly = true)
    public FechamentoEncomendaResponse obterPorEncomenda(Long idEncomenda) {
        return FechamentoEncomendaResponse.of(obter(idEncomenda));
    }

    /**
     * Acumula horas validadas de um funcionário para a encomenda, alimentando
     * o custeio por ordem. Reagrupamento por (encomenda, funcionário, data).
     */
    @Transactional
    public void registrarHorasValidadas(Long idEncomenda, Long idFuncionario, Integer nivelAcesso,
                                        BigDecimal horas, LocalDate data) {
        HorasEncomenda linha = horasRepository.findFirstByIdEncomendaAndIdFuncionarioAndDataRegistro(
                idEncomenda, idFuncionario, data)
                .orElseGet(() -> {
                    HorasEncomenda nova = new HorasEncomenda();
                    nova.setIdEncomenda(idEncomenda);
                    nova.setIdFuncionario(idFuncionario);
                    nova.setDataRegistro(data);
                    nova.setHoras(BigDecimal.ZERO);
                    return nova;
                });
        if (linha.getNivelAcesso() == null && nivelAcesso != null) {
            linha.setNivelAcesso(nivelAcesso);
        }
        linha.setHoras(linha.getHoras().add(horas));
        horasRepository.save(linha);

        fechamentoRepository.findByIdEncomenda(idEncomenda)
                .ifPresent(f -> {
                    f.adicionarHorasValidadas(horas);
                    fechamentoRepository.save(f);
                });
    }

    private FechamentoEncomenda obter(Long idEncomenda) {
        return fechamentoRepository.findByIdEncomenda(idEncomenda)
                .orElseThrow(() -> new ResourceNotFoundException("Fechamento não encontrado: " + idEncomenda));
    }
}