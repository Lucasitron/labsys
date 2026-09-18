package com.fablab.rh.service;

import com.fablab.rh.dto.CertificadoEmitidoResponse;
import com.fablab.rh.dto.DecisaoSolicitacaoRequest;
import com.fablab.rh.dto.HorasDisponiveisResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.SolicitacaoCertificadoResponse;
import com.fablab.rh.dto.SolicitarCertificadoRequest;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.CertificadoEmitido;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.HoraConsolidada;
import com.fablab.rh.entity.SolicitacaoCertificado;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.StatusSolicitacao;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.CertificadoMapper;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.CertificadoEmitidoRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HoraConsolidadaRepository;
import com.fablab.rh.repository.SolicitacaoCertificadoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Certificados de horas: solicitação pelo funcionário, decisão (aprovação ou
 * rejeição) pelo Admin e consulta de certificados emitidos.
 */
@Service
public class CertificadoService {

    private final SolicitacaoCertificadoRepository solicitacaoRepository;
    private final CertificadoEmitidoRepository certificadoRepository;
    private final HoraConsolidadaRepository horaConsolidadaRepository;
    private final ApontamentoHorasRepository apontamentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final RhEventPublisher eventPublisher;

    public CertificadoService(SolicitacaoCertificadoRepository solicitacaoRepository,
                              CertificadoEmitidoRepository certificadoRepository,
                              HoraConsolidadaRepository horaConsolidadaRepository,
                              ApontamentoHorasRepository apontamentoRepository,
                              FuncionarioRepository funcionarioRepository,
                              RhEventPublisher eventPublisher) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.certificadoRepository = certificadoRepository;
        this.horaConsolidadaRepository = horaConsolidadaRepository;
        this.apontamentoRepository = apontamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Abre uma solicitação de certificado para o funcionário autenticado. */
    @Transactional
    public SolicitacaoCertificadoResponse solicitar(SolicitarCertificadoRequest request, RhPrincipal principal) {
        Funcionario funcionario = obterFuncionarioDoPrincipal(principal);

        BigDecimal disponiveis = horasDisponiveis(funcionario.getId());
        if (request.horasSolicitadas().compareTo(disponiveis) > 0) {
            throw new IllegalArgumentException(
                    "Horas solicitadas excedem as horas disponíveis (" + disponiveis + "h)");
        }

        SolicitacaoCertificado solicitacao = new SolicitacaoCertificado();
        solicitacao.setFuncionario(funcionario);
        solicitacao.setTipoCertificado(request.tipoCertificado());
        solicitacao.setDataSolicitacao(LocalDateTime.now());
        solicitacao.setHorasSolicitadas(request.horasSolicitadas());
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);

        SolicitacaoCertificado salva = solicitacaoRepository.save(solicitacao);
        eventPublisher.publishCertificadoSolicitado(CertificadoMapper.toSolicitadoEvent(salva));
        return CertificadoMapper.toResponse(salva);
    }

    /** Lista solicitações: Admin vê todas; o funcionário vê apenas as suas. */
    @Transactional(readOnly = true)
    public List<SolicitacaoCertificadoResponse> listarSolicitacoes(StatusSolicitacao status,
                                                                   RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }

        List<SolicitacaoCertificado> solicitacoes;
        if (principal.isAdmin()) {
            solicitacoes = status != null
                    ? solicitacaoRepository.findByStatusOrderByDataSolicitacaoDesc(status)
                    : solicitacaoRepository.findAllByOrderByDataSolicitacaoDesc();
        } else {
            solicitacoes = status != null
                    ? solicitacaoRepository
                    .findByFuncionarioIdAndStatusOrderByDataSolicitacaoDesc(principal.idFuncionario(), status)
                    : solicitacaoRepository.findByFuncionarioIdOrderByDataSolicitacaoDesc(
                    principal.idFuncionario());
        }
        return solicitacoes.stream().map(CertificadoMapper::toResponse).toList();
    }

    /**
     * Aprova a solicitação: emite o certificado, consolida as horas disponíveis
     * (apontamentos {@code VALIDADO} e {@code consolidado = false}) e publica o
     * evento de aprovação.
     */
    @Transactional
    public CertificadoEmitidoResponse aprovar(Long id, DecisaoSolicitacaoRequest request, RhPrincipal principal) {
        SolicitacaoCertificado solicitacao = obterSolicitacaoPendenteParaDecisao(id);
        Funcionario admin = obterFuncionarioDoPrincipal(principal);

        BigDecimal disponiveis = horasDisponiveis(solicitacao.getFuncionario().getId());
        if (solicitacao.getHorasSolicitadas().compareTo(disponiveis) > 0) {
            throw new IllegalArgumentException(
                    "Horas disponíveis insuficientes para aprovar a solicitação " + id
                            + " (disponíveis: " + disponiveis + "h)");
        }

        CertificadoEmitido certificado = emitirCertificado(solicitacao, admin, request);
        return CertificadoMapper.toResponse(
                certificado,
                horaConsolidadaRepository.findByCertificadoIdCertificado(certificado.getIdCertificado()));
    }

    /** Emite o certificado e consolida todos os apontamentos disponíveis do funcionário. */
    private CertificadoEmitido emitirCertificado(SolicitacaoCertificado solicitacao, Funcionario admin,
                                                 DecisaoSolicitacaoRequest request) {
        solicitacao.setStatus(StatusSolicitacao.APROVADO);
        solicitacao.setIdAdminAprovador(admin);
        solicitacao.setDataDecisao(LocalDateTime.now());
        if (request != null && request.observacao() != null) {
            solicitacao.setObservacao(request.observacao());
        }
        solicitacaoRepository.save(solicitacao);

        CertificadoEmitido certificado = new CertificadoEmitido();
        certificado.setSolicitacao(solicitacao);
        certificado.setFuncionario(solicitacao.getFuncionario());
        certificado.setTipoCertificado(solicitacao.getTipoCertificado());
        certificado.setHorasCertificadas(solicitacao.getHorasSolicitadas());
        certificado.setDataEmissao(LocalDateTime.now());
        certificado.setCodigoVerificacao(UUID.randomUUID());
        CertificadoEmitido emitida = certificadoRepository.save(certificado);

        List<ApontamentoHoras> disponiveis = apontamentoRepository
                .findByFuncionarioIdAndStatusAndConsolidadoFalse(
                        solicitacao.getFuncionario().getId(), StatusApontamento.VALIDADO);
        for (ApontamentoHoras apontamento : disponiveis) {
            apontamento.setConsolidado(true);
            apontamentoRepository.save(apontamento);

            HoraConsolidada consolidada = new HoraConsolidada();
            consolidada.setCertificado(emitida);
            consolidada.setApontamento(apontamento);
            consolidada.setHoras(apontamento.getHorasTrabalhadas());
            consolidada.setDataConsolidacao(LocalDateTime.now());
            horaConsolidadaRepository.save(consolidada);
        }

        eventPublisher.publishCertificadoAprovado(CertificadoMapper.toAprovadoEvent(emitida));
        return emitida;
    }

    /** Rejeita a solicitação e notifica o funcionário. */
    @Transactional
    public SolicitacaoCertificadoResponse rejeitar(Long id, DecisaoSolicitacaoRequest request,
                                                   RhPrincipal principal) {
        SolicitacaoCertificado solicitacao = obterSolicitacaoPendenteParaDecisao(id);
        Funcionario admin = obterFuncionarioDoPrincipal(principal);

        solicitacao.setStatus(StatusSolicitacao.REJEITADO);
        solicitacao.setIdAdminAprovador(admin);
        solicitacao.setDataDecisao(LocalDateTime.now());
        if (request != null && request.observacao() != null) {
            solicitacao.setObservacao(request.observacao());
        }
        SolicitacaoCertificado salva = solicitacaoRepository.save(solicitacao);

        eventPublisher.publishCertificadoRejeitado(CertificadoMapper.toRejeitadoEvent(salva));
        return CertificadoMapper.toResponse(salva);
    }

    /** Lista certificados emitidos: Admin vê todos; o funcionário vê apenas os seus. */
    @Transactional(readOnly = true)
    public List<CertificadoEmitidoResponse> listarEmitidos(RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        List<CertificadoEmitido> emitidos = principal.isAdmin()
                ? certificadoRepository.findAllByOrderByDataEmissaoDesc()
                : certificadoRepository.findByFuncionarioIdOrderByDataEmissaoDesc(principal.idFuncionario());
        return emitidos.stream()
                .map(c -> CertificadoMapper.toResponse(c, obterConsolidadas(c.getIdCertificado())))
                .toList();
    }

    /** Detalha um certificado emitido; somente o dono ou o Admin. */
    @Transactional(readOnly = true)
    public CertificadoEmitidoResponse obterEmitido(Long id, RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        CertificadoEmitido certificado = certificadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificado não encontrado: " + id));
        if (!principal.isAdmin()
                && !principal.idFuncionario().equals(certificado.getFuncionario().getId())) {
            throw new ForbiddenException("Acesso apenas aos seus próprios certificados");
        }
        return CertificadoMapper.toResponse(certificado, obterConsolidadas(id));
    }

    /** Total de horas disponíveis (não consolidadas) do usuário autenticado. */
    @Transactional(readOnly = true)
    public HorasDisponiveisResponse horasDisponiveis(RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        return new HorasDisponiveisResponse(
                principal.idFuncionario(), horasDisponiveis(principal.idFuncionario()));
    }

    /** Soma as horas válidas ainda não consolidadas de um funcionário. */
    private BigDecimal horasDisponiveis(Long idFuncionario) {
        BigDecimal total = apontamentoRepository.sumHorasDisponiveis(idFuncionario);
        return total == null ? BigDecimal.ZERO : total;
    }

    private List<HoraConsolidada> obterConsolidadas(Long idCertificado) {
        return horaConsolidadaRepository.findByCertificadoIdCertificado(idCertificado);
    }

    /** Obtém a solicitação com bloqueio pessimista para impedir dupla decisão. */
    private SolicitacaoCertificado obterSolicitacaoPendenteParaDecisao(Long id) {
        SolicitacaoCertificado solicitacao = solicitacaoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada: " + id));
        if (solicitacao.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new IllegalArgumentException("Solicitação já decidida: " + id);
        }
        return solicitacao;
    }

    private Funcionario obterFuncionarioDoPrincipal(RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        return funcionarioRepository.findById(principal.idFuncionario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Funcionário não encontrado: " + principal.idFuncionario()));
    }
}