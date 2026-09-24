package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.DoacaoDtos.DoacaoRequest;
import com.fablab.financeiro.dto.DoacaoDtos.DoacaoResponse;
import com.fablab.financeiro.entity.DoacaoRecurso;
import com.fablab.financeiro.entity.TipoDoacao;
import com.fablab.financeiro.repository.DoacaoRecursoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro e listagem de doações e recursos de projetos. */
@Service
public class DoacaoRecursoService {

    private final DoacaoRecursoRepository repository;

    public DoacaoRecursoService(DoacaoRecursoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DoacaoResponse registrar(DoacaoRequest request) {
        DoacaoRecurso entity = new DoacaoRecurso();
        entity.setTipo(request.tipo());
        entity.setOrigem(request.origem().trim());
        entity.setValor(request.valor());
        entity.setDataRecebimento(request.dataRecebimento());
        entity.setIdProjetoAssociado(request.idProjetoAssociado());
        return DoacaoResponse.of(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DoacaoResponse> listar(TipoDoacao tipo, LocalDate dataInicio, LocalDate dataFim) {
        List<DoacaoRecurso> entities;
        if (tipo != null && dataInicio != null && dataFim != null) {
            entities = repository.findByTipoAndDataRecebimentoBetween(tipo, dataInicio, dataFim);
        } else if (tipo != null) {
            entities = repository.findByTipo(tipo);
        } else if (dataInicio != null && dataFim != null) {
            entities = repository.findByDataRecebimentoBetween(dataInicio, dataFim);
        } else {
            entities = repository.findAll();
        }
        return entities.stream().map(DoacaoResponse::of).toList();
    }
}
