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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
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

        BigDecimal horas = resolverHoras(request);
        ApontamentoHoras apontamento = ApontamentoHorasMapper.toEntity(funcionario, request, horas);
        return ApontamentoHorasMapper.toResponse(apontamentoRepository.save(apontamento));
    }

    /**
     * Lista apontamentos com filtros. Admin vê todos; demais veem apenas os
     * próprios (exige vínculo com funcionário).
     */
    @Transactional(readOnly = true)
    public List<ApontamentoHorasResponse> listar(StatusApontamento status, YearMonth periodo,
                                                 RhPrincipal principal) {
        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        Long funcionarioId = principal.isAdmin() ? null : principal.idFuncionario();
        LocalDate inicio = periodo == null ? null : periodo.atDay(1);
        LocalDate fim = periodo == null ? null : periodo.atEndOfMonth();
        return apontamentoRepository.buscarComFiltros(funcionarioId, status, inicio, fim).stream()
                .map(ApontamentoHorasMapper::toResponse)
                .toList();
    }

    /**
     * Resolve as horas do apontamento: com início+fim, recalcula no servidor
     * (ignora o valor enviado); sem eles, exige {@code horasTrabalhadas}.
     */
    static BigDecimal resolverHoras(ApontamentoHorasRequest request) {
        LocalTime inicio = request.horaInicio();
        LocalTime fim = request.horaFim();
        if (inicio != null && fim != null) {
            if (!fim.isAfter(inicio)) {
                throw new IllegalArgumentException("Hora fim deve ser posterior à hora início");
            }
            return BigDecimal.valueOf(Duration.between(inicio, fim).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }
        if (inicio != null || fim != null) {
            throw new IllegalArgumentException("Informe hora início e hora fim juntos");
        }
        if (request.horasTrabalhadas() == null) {
            throw new IllegalArgumentException("Informe horasTrabalhadas ou hora início e hora fim");
        }
        if (request.horasTrabalhadas().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("horasTrabalhadas deve ser maior que zero");
        }
        return request.horasTrabalhadas();
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
        apontamento.setMotivoRejeicao(request.status() == StatusApontamento.REJEITADO
                ? request.motivo() : null);
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