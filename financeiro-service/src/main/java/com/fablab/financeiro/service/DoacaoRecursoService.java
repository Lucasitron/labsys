package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.DoacaoRecursoRequest;
import com.fablab.financeiro.dto.DoacaoRecursoResponse;
import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacaoRecurso;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro de doações e recursos de projetos universitários.
 */
@Service
public class DoacaoRecursoService {

    private final DoacaoRecursoRepository doacaoRepository;

    public DoacaoRecursoService(DoacaoRecursoRepository doacaoRepository) {
        this.doacaoRepository = doacaoRepository;
    }

    @Transactional
    public DoacaoRecursoResponse criar(DoacaoRecursoRequest request) {
        DoacaoRecurso doacao = new DoacaoRecurso();
        doacao.setTipo(request.tipo());
        doacao.setOrigem(request.origem());
        doacao.setValor(request.valor());
        doacao.setDataRecebimento(request.dataRecebimento());
        doacao.setIdProjetoAssociado(request.idProjetoAssociado());
        return DoacaoRecursoResponse.of(doacaoRepository.save(doacao));
    }

    @Transactional(readOnly = true)
    public List<DoacaoRecursoResponse> listar(TipoDoacaoRecurso tipo, LocalDate dataInicio, LocalDate dataFim) {
        List<DoacaoRecurso> resultados;
        if (dataFim != null) {
            LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.EPOCH;
            resultados = doacaoRepository.findByDataRecebimentoBetween(inicio, dataFim);
        } else {
            resultados = doacaoRepository.findAll();
        }
        if (tipo != null) {
            resultados = resultados.stream().filter(d -> d.getTipo() == tipo).toList();
        }
        return resultados.stream().map(DoacaoRecursoResponse::of).toList();
    }
}