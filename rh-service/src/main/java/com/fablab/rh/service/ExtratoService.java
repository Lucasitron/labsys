package com.fablab.rh.service;

import com.fablab.rh.dto.ExtratoMensalHorasEvent;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compilação do extrato mensal de horas e publicação do evento
 * {@code extrato.mensal.horas.event} para o Notification Service.
 */
@Service
public class ExtratoService {

    private static final DateTimeFormatter MES_REFERENCIA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final FuncionarioRepository funcionarioRepository;
    private final RegistroPontoDiarioRepository pontoRepository;
    private final ApontamentoHorasRepository apontamentoRepository;
    private final RhEventPublisher eventPublisher;

    public ExtratoService(FuncionarioRepository funcionarioRepository,
                          RegistroPontoDiarioRepository pontoRepository,
                          ApontamentoHorasRepository apontamentoRepository,
                          RhEventPublisher eventPublisher) {
        this.funcionarioRepository = funcionarioRepository;
        this.pontoRepository = pontoRepository;
        this.apontamentoRepository = apontamentoRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Gera o extrato do mês anterior (usado pelo job agendado). */
    @Transactional
    public void gerarExtratoMensal() {
        gerarExtratoMensal(YearMonth.now().minusMonths(1));
    }

    /**
     * Compila o extrato do mês de referência para cada funcionário ativo
     * (excluindo Recrutandos) e publica o evento.
     *
     * @param mesReferencia mês de referência do extrato
     */
    @Transactional
    public void gerarExtratoMensal(YearMonth mesReferencia) {
        LocalDate inicio = mesReferencia.atDay(1);
        LocalDate fim = mesReferencia.atEndOfMonth();
        String referencia = mesReferencia.format(MES_REFERENCIA);

        List<Funcionario> ativos = funcionarioRepository.findByNivelAcessoNot(NivelAcesso.RECRUTANDO).stream()
                .filter(f -> f.getPessoa().getStatus() == PessoaStatus.ATIVO)
                .toList();

        for (Funcionario funcionario : ativos) {
            BigDecimal presencia = orZero(pontoRepository.sumTotalHorasPeriodo(
                    funcionario.getId(), inicio, fim));
            BigDecimal encomenda = orZero(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(
                    funcionario.getId(), TipoApontamento.ENCOMENDA, inicio, fim));
            BigDecimal projeto = orZero(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(
                    funcionario.getId(), TipoApontamento.PROJETO, inicio, fim));
            BigDecimal disponiveis = orZero(apontamentoRepository.sumHorasDisponiveis(funcionario.getId()));

            eventPublisher.publishExtratoMensal(new ExtratoMensalHorasEvent(
                    funcionario.getId(),
                    funcionario.getPessoa().getNomeCompleto(),
                    referencia,
                    presencia,
                    encomenda,
                    projeto,
                    disponiveis));
        }
    }

    private BigDecimal orZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}