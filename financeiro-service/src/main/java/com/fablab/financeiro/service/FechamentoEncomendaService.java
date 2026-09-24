package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CusteioDtos.FechamentoRequest;
import com.fablab.financeiro.dto.CusteioDtos.FechamentoResponse;
import com.fablab.financeiro.dto.FinanceiroEventos.EncomendaCriadaEvent;
import com.fablab.financeiro.dto.FinanceiroEventos.HorasValidadasEvent;
import com.fablab.financeiro.entity.FechamentoEncomenda;
import com.fablab.financeiro.entity.HorasEncomenda;
import com.fablab.financeiro.entity.StatusFechamento;
import com.fablab.financeiro.exception.ResourceNotFoundException;
import com.fablab.financeiro.repository.FechamentoEncomendaRepository;
import com.fablab.financeiro.repository.HorasEncomendaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fechamento de encomenda (congelamento — D-5): abertura manual ou automática
 * idempotente via {@code encomenda.criada.event}, consulta e acúmulo de horas
 * validadas do RH.
 */
@Service
public class FechamentoEncomendaService {

    private static final Logger log = LoggerFactory.getLogger(FechamentoEncomendaService.class);

    private final FechamentoEncomendaRepository fechamentoRepository;
    private final HorasEncomendaRepository horasRepository;

    public FechamentoEncomendaService(FechamentoEncomendaRepository fechamentoRepository,
                                      HorasEncomendaRepository horasRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.horasRepository = horasRepository;
    }

    @Transactional
    public FechamentoResponse criar(FechamentoRequest request, Long idUsuario) {
        if (fechamentoRepository.existsByIdEncomenda(request.idEncomenda())) {
            throw new IllegalArgumentException("Encomenda já possui fechamento. Alterações geram nova ordem.");
        }
        FechamentoEncomenda entity = new FechamentoEncomenda();
        entity.setIdEncomenda(request.idEncomenda());
        entity.setHorasEstimadas(request.horasEstimadas());
        entity.setValorFechado(request.valorFechado());
        entity.setDataFechamento(request.dataFechamento() != null ? request.dataFechamento() : LocalDate.now());
        entity.setStatus(StatusFechamento.ABERTA);
        entity.setHorasValidadas(BigDecimal.ZERO);
        FechamentoEncomenda salvo = fechamentoRepository.save(entity);
        log.info("Auditoria: usuário {} criou fechamento da encomenda {} (valor {}, horas estimadas {})",
                idUsuario, request.idEncomenda(), request.valorFechado(), request.horasEstimadas());
        return FechamentoResponse.of(salvo);
    }

    /** Abertura automática idempotente a partir de {@code encomenda.criada.event}. */
    @Transactional
    public FechamentoResponse criarDoEvento(EncomendaCriadaEvent event) {
        return fechamentoRepository.findByIdEncomenda(event.idEncomenda())
                .map(FechamentoResponse::of)
                .orElseGet(() -> {
                    FechamentoEncomenda entity = new FechamentoEncomenda();
                    entity.setIdEncomenda(event.idEncomenda());
                    entity.setHorasEstimadas(BigDecimal.ZERO);
                    entity.setValorFechado(event.valorFinal() != null ? event.valorFinal() : BigDecimal.ZERO);
                    entity.setDataFechamento(event.dataCriacao() != null ? event.dataCriacao() : LocalDate.now());
                    entity.setStatus(StatusFechamento.ABERTA);
                    entity.setHorasValidadas(BigDecimal.ZERO);
                    return FechamentoResponse.of(fechamentoRepository.save(entity));
                });
    }

    @Transactional(readOnly = true)
    public FechamentoResponse consultar(Integer idEncomenda) {
        return FechamentoResponse.of(buscar(idEncomenda));
    }

    @Transactional(readOnly = true)
    public List<FechamentoResponse> listar() {
        return fechamentoRepository.findAll().stream().map(FechamentoResponse::of).toList();
    }

    /**
     * Acumula horas validadas: upsert em {@code horas_encomenda} por
     * (encomenda, funcionário, data) + incremento de {@code horas_validadas}.
     * Horas sem nível seguem para o custeio com default nível 2 (Voluntário).
     */
    @Transactional
    public void registrarHorasValidadas(HorasValidadasEvent event) {
        FechamentoEncomenda fechamento = buscar(event.idEncomenda());
        LocalDate data = event.dataRegistro() != null ? event.dataRegistro() : LocalDate.now();
        HorasEncomenda linha = horasRepository
                .findByIdEncomendaAndIdFuncionarioAndDataRegistro(event.idEncomenda(), event.idFuncionario(), data)
                .orElseGet(() -> {
                    HorasEncomenda nova = new HorasEncomenda();
                    nova.setIdEncomenda(event.idEncomenda());
                    nova.setIdFuncionario(event.idFuncionario());
                    nova.setNivelAcesso(event.nivelAcesso());
                    nova.setHoras(BigDecimal.ZERO);
                    nova.setDataRegistro(data);
                    return nova;
                });
        linha.setHoras(linha.getHoras().add(event.horas()));
        if (event.nivelAcesso() != null) {
            linha.setNivelAcesso(event.nivelAcesso());
        }
        horasRepository.save(linha);
        fechamento.setHorasValidadas(fechamento.getHorasValidadas().add(event.horas()));
        fechamentoRepository.save(fechamento);
    }

    /**
     * Alteração de encomenda (D-5): encerra a ordem atual (custo congelado) e
     * abre nova ordem com novas estimativas e valor — nunca edita o congelado.
     */
    @Transactional
    public FechamentoResponse novaOrdem(Integer idEncomenda, FechamentoRequest request, Long idUsuario) {
        FechamentoEncomenda atual = buscar(idEncomenda);
        atual.setStatus(StatusFechamento.CONCLUIDA);
        fechamentoRepository.save(atual);
        log.info("Auditoria: usuário {} encerrou a ordem da encomenda {} e abriu nova ordem",
                idUsuario, idEncomenda);
        return criar(request, idUsuario);
    }

    @Transactional
    public FechamentoResponse cancelar(Integer idEncomenda, Long idUsuario) {
        FechamentoEncomenda entity = buscar(idEncomenda);
        entity.setStatus(StatusFechamento.CANCELADA);
        log.info("Auditoria: usuário {} cancelou o fechamento da encomenda {}", idUsuario, idEncomenda);
        return FechamentoResponse.of(fechamentoRepository.save(entity));
    }

    private FechamentoEncomenda buscar(Integer idEncomenda) {
        return fechamentoRepository.findByIdEncomenda(idEncomenda)
                .orElseThrow(() -> new ResourceNotFoundException("Fechamento da encomenda não encontrado"));
    }
}
