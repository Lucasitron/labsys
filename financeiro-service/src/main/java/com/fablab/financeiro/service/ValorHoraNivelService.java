package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.ValorHoraNivelRequest;
import com.fablab.financeiro.dto.ValorHoraNivelResponse;
import com.fablab.financeiro.entity.ValorHoraNivel;
import com.fablab.financeiro.repository.ValorHoraNivelRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valor da hora de trabalho por nível de acesso (0-3) e sua vigência.
 */
@Service
public class ValorHoraNivelService {

    private static final int NIVEIS_SUPORTADOS = 4;

    private final ValorHoraNivelRepository valorHoraRepository;

    public ValorHoraNivelService(ValorHoraNivelRepository valorHoraRepository) {
        this.valorHoraRepository = valorHoraRepository;
    }

    @Transactional
    public ValorHoraNivelResponse definir(ValorHoraNivelRequest request) {
        ValorHoraNivel valorHora = new ValorHoraNivel();
        valorHora.setNivelAcesso(request.nivelAcesso());
        valorHora.setValorHora(request.valorHora());
        valorHora.setDataVigencia(request.dataVigencia());
        return ValorHoraNivelResponse.of(valorHoraRepository.save(valorHora));
    }

    /**
     * Lista a taxa vigente de cada nível configurado (a mais recente por nível).
     */
    @Transactional(readOnly = true)
    public List<ValorHoraNivelResponse> listarVigentes() {
        List<ValorHoraNivelResponse> resultado = new ArrayList<>();
        for (int nivel = 0; nivel < NIVEIS_SUPORTADOS; nivel++) {
            obterValorHoraPorNivel(nivel)
                    .ifPresent(v -> resultado.add(ValorHoraNivelResponse.of(v)));
        }
        return resultado;
    }

    /**
     * Obtém a taxa vigente (data mais recente) para um nível de acesso.
     */
    @Transactional(readOnly = true)
    public Optional<ValorHoraNivel> obterValorHoraPorNivel(Integer nivel) {
        return valorHoraRepository.findFirstByNivelAcessoOrderByDataVigenciaDesc(nivel);
    }
}