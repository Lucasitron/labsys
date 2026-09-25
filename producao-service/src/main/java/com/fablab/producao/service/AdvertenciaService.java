package com.fablab.producao.service;

import com.fablab.producao.dto.AdvertenciaLimiteEvent;
import com.fablab.producao.dto.AdvertenciaRegistradaEvent;
import com.fablab.producao.dto.AdvertenciaRequest;
import com.fablab.producao.dto.AdvertenciaResponse;
import com.fablab.producao.entity.AdvertenciaMembro;
import com.fablab.producao.entity.Inspecao5S;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.AdvertenciaMembroRepository;
import com.fablab.producao.repository.Inspecao5SRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de advertências e penalidades. Cada membro possui um contador
 * acumulado; a partir da terceira advertência a penalidade deve ser avaliada
 * pelo RH (o contador é enviado no evento de notificação).
 */
@Service
public class AdvertenciaService {

    public static final int LIMITE_PENALIDADE = 3;

    private final AdvertenciaMembroRepository advertenciaRepository;
    private final Inspecao5SRepository inspecaoRepository;
    private final ProducaoEventPublisher eventPublisher;
    private final AcessoService acessoService;

    public AdvertenciaService(AdvertenciaMembroRepository advertenciaRepository,
                              Inspecao5SRepository inspecaoRepository,
                              ProducaoEventPublisher eventPublisher,
                              AcessoService acessoService) {
        this.advertenciaRepository = advertenciaRepository;
        this.inspecaoRepository = inspecaoRepository;
        this.eventPublisher = eventPublisher;
        this.acessoService = acessoService;
    }

    @Transactional(readOnly = true)
    public List<AdvertenciaResponse> listar(Long idFuncionario) {
        List<AdvertenciaMembro> advertencias = idFuncionario == null
                ? advertenciaRepository.findAll()
                : advertenciaRepository.findByIdFuncionarioOrderByDataDesc(idFuncionario);
        return advertencias.stream().map(AdvertenciaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdvertenciaResponse buscar(Long id) {
        return AdvertenciaResponse.from(obter(id));
    }

    @Transactional
    public AdvertenciaResponse registrar(AdvertenciaRequest request) {
        Inspecao5S inspecao = request.idInspecao() == null ? null : inspecaoRepository.findById(request.idInspecao())
                .orElseThrow(() -> new ResourceNotFoundException("Inspeção 5S", request.idInspecao()));
        return registrarInterno(request.idFuncionario(), inspecao, request.data(), request.motivo(), request.tipo());
    }

    /** Usado pelo fluxo de inspeção 5S quando há não conformidade. */
    @Transactional
    public AdvertenciaResponse registrarAutomatica(Long idFuncionario, Inspecao5S inspecao, String motivo) {
        return registrarInterno(idFuncionario, inspecao, LocalDate.now(), motivo,
                com.fablab.producao.entity.TipoAdvertencia.VERBAL);
    }

    private AdvertenciaResponse registrarInterno(Long idFuncionario,
                                                 Inspecao5S inspecao,
                                                 LocalDate data,
                                                 String motivo,
                                                 com.fablab.producao.entity.TipoAdvertencia tipo) {
        AdvertenciaMembro advertencia = new AdvertenciaMembro();
        advertencia.setIdFuncionario(idFuncionario);
        advertencia.setInspecao(inspecao);
        advertencia.setData(data == null ? LocalDate.now() : data);
        advertencia.setMotivo(motivo);
        advertencia.setTipo(tipo);
        advertencia.setContador((int) advertenciaRepository.countByIdFuncionario(idFuncionario) + 1);
        advertencia.setIdAdminRegistrou(acessoService.idUsuario());
        AdvertenciaMembro salva = advertenciaRepository.save(advertencia);
        eventPublisher.publicarAdvertencia(
                new AdvertenciaRegistradaEvent(salva.getIdFuncionario(), salva.getContador(), salva.getMotivo()));
        if (salva.getContador() >= LIMITE_PENALIDADE) {
            eventPublisher.publicarAdvertenciaLimite(
                    new AdvertenciaLimiteEvent(salva.getIdFuncionario(), salva.getContador(), salva.getMotivo()));
        }
        return AdvertenciaResponse.from(salva);
    }

    private AdvertenciaMembro obter(Long id) {
        return advertenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advertência", id));
    }
}