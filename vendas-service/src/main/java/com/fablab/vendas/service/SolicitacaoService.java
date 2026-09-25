package com.fablab.vendas.service;

import com.fablab.vendas.dto.SolicitacaoDtos.DecisaoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoListaResponse;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoRequest;
import com.fablab.vendas.dto.SolicitacaoDtos.SolicitacaoResponse;
import com.fablab.vendas.dto.VendasPrincipal;
import com.fablab.vendas.entity.SolicitacaoEdicao;
import com.fablab.vendas.entity.StatusSolicitacao;
import com.fablab.vendas.entity.TipoAlvoSolicitacao;
import com.fablab.vendas.entity.TipoSolicitacao;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.exception.ForbiddenException;
import com.fablab.vendas.exception.ResourceNotFoundException;
import com.fablab.vendas.repository.SolicitacaoEdicaoRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitações de edição (contrato mínimo — D-4 REPORTADO): qualquer usuário
 * solicita; decisão só Admin (papel "responsável de Vendas" sem lastro na
 * spec — REPORTADO, não inventado). A aplicação da edição aprovada é manual.
 */
@Service
public class SolicitacaoService {

    private final SolicitacaoEdicaoRepository solicitacaoRepository;

    public SolicitacaoService(SolicitacaoEdicaoRepository solicitacaoRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional
    public SolicitacaoResponse solicitar(SolicitacaoRequest request, VendasPrincipal principal) {
        TipoSolicitacao tipo;
        TipoAlvoSolicitacao alvoTipo;
        try {
            tipo = TipoSolicitacao.valueOf(request.tipo());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Tipo inválido (ALTERACAO_DADOS, MUDANCA_STATUS, MOVER_ENCOMENDA ou OUTRA)");
        }
        try {
            alvoTipo = TipoAlvoSolicitacao.valueOf(request.alvoTipo());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Alvo inválido (CLIENTE, ORCAMENTO ou ENCOMENDA)");
        }
        SolicitacaoEdicao solicitacao = new SolicitacaoEdicao();
        solicitacao.setTipo(tipo);
        solicitacao.setAlvoTipo(alvoTipo);
        solicitacao.setAlvoId(request.alvoId());
        solicitacao.setCampo(request.campo());
        solicitacao.setValorAtual(request.valorAtual());
        solicitacao.setValorProposto(request.valorProposto());
        solicitacao.setJustificativa(request.justificativa());
        solicitacao.setStatus(StatusSolicitacao.Pendente);
        solicitacao.setSolicitanteId(principal.idPessoa());
        solicitacao.setDataCriacao(LocalDateTime.now());
        return paraResponse(solicitacaoRepository.save(solicitacao));
    }

    @Transactional(readOnly = true)
    public SolicitacaoListaResponse listar(String status) {
        List<SolicitacaoEdicao> todas = solicitacaoRepository.findAll();
        if (status != null && !status.isBlank()) {
            StatusSolicitacao filtro;
            try {
                filtro = StatusSolicitacao.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Status inválido (Pendente, Aprovada ou Rejeitada)");
            }
            todas = todas.stream().filter(s -> s.getStatus() == filtro).toList();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StatusSolicitacao s : StatusSolicitacao.values()) {
            counts.put(s.name(), solicitacaoRepository.countByStatus(s));
        }
        return new SolicitacaoListaResponse(todas.stream().map(this::paraResponse).toList(), counts);
    }

    @Transactional
    public SolicitacaoResponse decidir(Long id, DecisaoRequest request, VendasPrincipal principal) {
        if (!principal.isAdmin()) {
            throw new ForbiddenException("Apenas Admin pode decidir solicitações");
        }
        SolicitacaoEdicao solicitacao = solicitacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
        if (solicitacao.getStatus() != StatusSolicitacao.Pendente) {
            throw new ConflitoException("Solicitação já decidida");
        }
        String decisao = request.decisao() == null ? "" : request.decisao().trim().toUpperCase();
        if (decisao.equals("APROVAR")) {
            solicitacao.setStatus(StatusSolicitacao.Aprovada);
        } else if (decisao.equals("REJEITAR")) {
            if (request.motivo() == null || request.motivo().isBlank()) {
                throw new IllegalArgumentException("O motivo é obrigatório ao rejeitar");
            }
            solicitacao.setStatus(StatusSolicitacao.Rejeitada);
        } else {
            throw new IllegalArgumentException("Decisão inválida (APROVAR ou REJEITAR)");
        }
        solicitacao.setDecididoPor(principal.idPessoa());
        solicitacao.setMotivoDecisao(request.motivo());
        solicitacao.setDataDecisao(LocalDateTime.now());
        return paraResponse(solicitacaoRepository.save(solicitacao));
    }

    private SolicitacaoResponse paraResponse(SolicitacaoEdicao s) {
        return new SolicitacaoResponse(s.getId(), s.getTipo().name(), s.getAlvoTipo().name(),
                s.getAlvoId(), s.getCampo(), s.getValorAtual(), s.getValorProposto(),
                s.getJustificativa(), s.getStatus().name(), s.getSolicitanteId(),
                s.getDecididoPor(), s.getMotivoDecisao(), s.getDataCriacao(), s.getDataDecisao());
    }
}
