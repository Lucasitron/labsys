package com.fablab.rh.service;

import com.fablab.rh.dto.ApontamentoHorasRequest;
import com.fablab.rh.dto.ApontamentoHorasResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.ValidacaoApontamentoRequest;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.RegistroPontoDiario;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.ApontamentoHorasMapper;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro e validação de apontamentos de horas (encomenda/projeto).
 */
@Service
public class ApontamentoHorasService {

    private final ApontamentoHorasRepository apontamentoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final RegistroPontoDiarioRepository pontoRepository;
    private final RhEventPublisher eventPublisher;

    public ApontamentoHorasService(ApontamentoHorasRepository apontamentoRepository,
                                   FuncionarioRepository funcionarioRepository,
                                   RegistroPontoDiarioRepository pontoRepository,
                                   RhEventPublisher eventPublisher) {
        this.apontamentoRepository = apontamentoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.pontoRepository = pontoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ApontamentoHorasResponse registrar(ApontamentoHorasRequest request, RhPrincipal principal) {
        var funcionario = funcionarioRepository.findById(request.idFuncionario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Funcionário não encontrado: " + request.idFuncionario()));

        if (principal != null && !principal.isAdmin()
                && (principal.idFuncionario() == null
                || !principal.idFuncionario().equals(funcionario.getId()))) {
            throw new ForbiddenException("Apenas o próprio funcionário pode registrar suas horas");
        }

        ApontamentoHoras apontamento = ApontamentoHorasMapper.toEntity(funcionario, request);
        return ApontamentoHorasMapper.toResponse(apontamentoRepository.save(apontamento));
    }

    @Transactional
    public ApontamentoHorasResponse validar(Long id, ValidacaoApontamentoRequest request, RhPrincipal principal) {
        ApontamentoHoras apontamento = obter(id);

        if (apontamento.getStatus() != StatusApontamento.PENDENTE) {
            throw new IllegalArgumentException("Apontamento já validado ou rejeitado: " + id);
        }
        if (request.status() == StatusApontamento.PENDENTE) {
            throw new IllegalArgumentException("Status deve ser VALIDADO ou REJEITADO");
        }
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }

        var admin = funcionarioRepository.findById(principal.idFuncionario())
                .orElseThrow(() -> new ForbiddenException("Funcionário responsável não encontrado"));

        if (request.status() == StatusApontamento.VALIDADO) {
            validarCoerencia(apontamento);
            eventPublisher.publishHorasValidadas(ApontamentoHorasMapper.toValidatedEvent(apontamento));
        }

        apontamento.setStatus(request.status());
        apontamento.setIdAdminValidador(admin);
        apontamento.setDataValidacao(Instant.now());
        return ApontamentoHorasMapper.toResponse(apontamentoRepository.save(apontamento));
    }

    /** Encomenda + projeto do dia não podem exceder as horas de presença. */
    private void validarCoerencia(ApontamentoHoras apontamento) {
        BigDecimal presenca = BigDecimal.ZERO;
        RegistroPontoDiario registro = pontoRepository
                .findByFuncionarioIdAndData(apontamento.getFuncionario().getId(), apontamento.getData())
                .orElse(null);
        if (registro != null && registro.getTotalHoras() != null) {
            presenca = registro.getTotalHoras();
        }

        BigDecimal jaApontado = apontamentoRepository.sumHorasApontadasPorDia(
                apontamento.getFuncionario().getId(), apontamento.getData());

        if (presenca.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Não há horas de presença registradas para validar horas em " + apontamento.getData());
        }
        if (jaApontado.compareTo(presenca) > 0) {
            throw new IllegalArgumentException(
                    "Horas apontadas (encomenda + projeto) excedem as horas de presença do dia "
                            + apontamento.getData());
        }
    }

    public ApontamentoHoras obter(Long id) {
        return apontamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apontamento não encontrado: " + id));
    }
}