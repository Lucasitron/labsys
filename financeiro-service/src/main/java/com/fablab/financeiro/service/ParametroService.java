package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.ParametroDtos.OverheadRequest;
import com.fablab.financeiro.dto.ParametroDtos.OverheadResponse;
import com.fablab.financeiro.dto.ParametroDtos.ValorHoraRequest;
import com.fablab.financeiro.dto.ParametroDtos.ValorHoraResponse;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.repository.ParametroOverheadRepository;
import com.fablab.financeiro.repository.ValorHoraNivelRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parâmetros de custeio: valor/hora por nível (0-3) e taxa de overhead, ambos
 * com vigência e auditoria em log (usuário/data — D-5).
 */
@Service
public class ParametroService {

    private static final Logger log = LoggerFactory.getLogger(ParametroService.class);

    private final ValorHoraNivelRepository valorHoraRepository;
    private final ParametroOverheadRepository overheadRepository;

    public ParametroService(ValorHoraNivelRepository valorHoraRepository,
                            ParametroOverheadRepository overheadRepository) {
        this.valorHoraRepository = valorHoraRepository;
        this.overheadRepository = overheadRepository;
    }

    @Transactional
    public ValorHoraResponse definirValorHora(ValorHoraRequest request, Long idUsuario) {
        ValorHoraNivel entity = new ValorHoraNivel();
        entity.setNivelAcesso(request.nivelAcesso());
        entity.setValorHora(request.valorHora());
        entity.setDataVigencia(request.dataVigencia());
        ValorHoraNivel salvo = valorHoraRepository.save(entity);
        log.info("Auditoria: usuário {} definiu valor/hora do nível {} = {} (vigência {})",
                idUsuario, request.nivelAcesso(), request.valorHora(), request.dataVigencia());
        return ValorHoraResponse.of(salvo);
    }

    /** Valores vigentes: o mais recente de cada nível (0-3). */
    @Transactional(readOnly = true)
    public List<ValorHoraResponse> valoresVigentes() {
        List<ValorHoraResponse> vigentes = new ArrayList<>();
        for (int nivel = 0; nivel <= 3; nivel++) {
            valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(nivel)
                    .map(ValorHoraResponse::of).ifPresent(vigentes::add);
        }
        return vigentes;
    }

    @Transactional(readOnly = true)
    public BigDecimal obterValorHoraVigente(int nivel) {
        return valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(nivel)
                .map(ValorHoraNivel::getValorHora)
                .orElseThrow(() -> new IllegalStateException(
                        "Valor/hora do nível " + nivel + " não configurado. Defina em /valores-hora antes do custeio."));
    }

    @Transactional
    public OverheadResponse definirOverhead(OverheadRequest request, Long idUsuario) {
        ParametroOverhead entity = new ParametroOverhead();
        entity.setValorTaxaHora(request.valorTaxaHora());
        entity.setDataVigencia(request.dataVigencia());
        ParametroOverhead salvo = overheadRepository.save(entity);
        log.info("Auditoria: usuário {} definiu taxa de overhead = {} (vigência {})",
                idUsuario, request.valorTaxaHora(), request.dataVigencia());
        return OverheadResponse.of(salvo);
    }

    @Transactional(readOnly = true)
    public OverheadResponse overheadVigente() {
        return overheadRepository.findFirstByOrderByDataVigenciaDesc()
                .map(OverheadResponse::of)
                .orElseThrow(() -> new IllegalStateException(
                        "Taxa de overhead não configurada. Defina em /parametros-overhead antes do custeio."));
    }

    @Transactional(readOnly = true)
    public BigDecimal obterTaxaVigente() {
        return overheadRepository.findFirstByOrderByDataVigenciaDesc()
                .map(ParametroOverhead::getValorTaxaHora)
                .orElseThrow(() -> new IllegalStateException(
                        "Taxa de overhead não configurada. Defina em /parametros-overhead antes do custeio."));
    }
}
