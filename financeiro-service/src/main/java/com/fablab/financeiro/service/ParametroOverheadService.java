package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.ParametroOverheadRequest;
import com.fablab.financeiro.dto.ParametroOverheadResponse;
import com.fablab.financeiro.entity.ParametroOverhead;
import com.fablab.financeiro.repository.ParametroOverheadRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parâmetro de taxa de overhead sobre horas de mão de obra.
 */
@Service
public class ParametroOverheadService {

    private final ParametroOverheadRepository parametroRepository;

    public ParametroOverheadService(ParametroOverheadRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Transactional
    public ParametroOverheadResponse definir(ParametroOverheadRequest request) {
        ParametroOverhead parametro = new ParametroOverhead();
        parametro.setValorTaxaHora(request.valorTaxaHora());
        parametro.setDataVigencia(request.dataVigencia());
        return ParametroOverheadResponse.of(parametroRepository.save(parametro));
    }

    /**
     * Taxa de overhead vigente (mais recente), quando configurada.
     */
    @Transactional(readOnly = true)
    public Optional<ParametroOverhead> taxaVigente() {
        return parametroRepository.findTopByOrderByDataVigenciaDesc();
    }
}