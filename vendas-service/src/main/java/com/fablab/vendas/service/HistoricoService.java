package com.fablab.vendas.service;

import com.fablab.vendas.dto.EncomendaDtos.HistoricoResponse;
import com.fablab.vendas.repository.HistoricoStatusEncomendaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leitura do histórico de movimentações do Kanban. */
@Service
public class HistoricoService {

    private final HistoricoStatusEncomendaRepository historicoRepository;

    public HistoricoService(HistoricoStatusEncomendaRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listar(Long idEncomenda) {
        return historicoRepository.findByIdEncomendaOrderByDataAlteracaoAsc(idEncomenda).stream()
                .map(h -> new HistoricoResponse(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                        h.getDataAlteracao(), h.getIdUsuario(), h.getObservacao()))
                .toList();
    }
}
