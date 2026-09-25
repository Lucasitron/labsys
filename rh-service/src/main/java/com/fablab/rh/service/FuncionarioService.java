package com.fablab.rh.service;

import com.fablab.rh.dto.FuncionarioHorasResponse;
import com.fablab.rh.dto.FuncionarioRequest;
import com.fablab.rh.dto.FuncionarioResponse;
import com.fablab.rh.dto.NivelAlteradoEvent;
import com.fablab.rh.dto.NivelAlteradoResponse;
import com.fablab.rh.dto.NivelRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.HistoricoNivel;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.RegistroPontoDiario;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.FuncionarioMapper;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HistoricoNivelRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão dos funcionários: vínculo com pessoa, listagem, evolução de nível de
 * acesso e totais de horas.
 */
@Service
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PessoaRepository pessoaRepository;
    private final HistoricoNivelRepository historicoRepository;
    private final RegistroPontoDiarioRepository pontoRepository;
    private final ApontamentoHorasRepository apontamentoRepository;
    private final RhEventPublisher eventPublisher;

    public FuncionarioService(FuncionarioRepository funcionarioRepository,
                              PessoaRepository pessoaRepository,
                              HistoricoNivelRepository historicoRepository,
                              RegistroPontoDiarioRepository pontoRepository,
                              ApontamentoHorasRepository apontamentoRepository,
                              RhEventPublisher eventPublisher) {
        this.funcionarioRepository = funcionarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.historicoRepository = historicoRepository;
        this.pontoRepository = pontoRepository;
        this.apontamentoRepository = apontamentoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public FuncionarioResponse vincular(FuncionarioRequest request) {
        var pessoa = pessoaRepository.findById(request.idPessoa())
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + request.idPessoa()));
        if (funcionarioRepository.existsByPessoaId(request.idPessoa())) {
            throw new IllegalArgumentException("Pessoa já vinculada a um funcionário: " + request.idPessoa());
        }
        Funcionario funcionario = FuncionarioMapper.toEntity(pessoa, request);
        return FuncionarioMapper.toResponse(funcionarioRepository.save(funcionario));
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listar(NivelAcesso nivel, String departamento) {
        List<Funcionario> funcionarios;
        if (nivel != null && departamento != null) {
            funcionarios = funcionarioRepository.findAll().stream()
                    .filter(f -> f.getNivelAcesso() == nivel
                            && f.getDepartamento() != null
                            && f.getDepartamento().equalsIgnoreCase(departamento))
                    .toList();
        } else if (nivel != null) {
            funcionarios = funcionarioRepository.findByNivelAcesso(nivel);
        } else if (departamento != null) {
            funcionarios = funcionarioRepository.findByDepartamentoIgnoreCase(departamento);
        } else {
            funcionarios = funcionarioRepository.findAll();
        }
        return funcionarios.stream().map(FuncionarioMapper::toResponse).toList();
    }

    @Transactional
    public NivelAlteradoResponse alterarNivel(Long id, NivelRequest request, RhPrincipal principal) {
        Funcionario funcionario = obter(id);

        if (principal == null || principal.idFuncionario() == null) {
            throw new ForbiddenException("Operação restrita a usuários vinculados a um funcionário");
        }
        boolean inalterado = funcionario.getNivelAcesso() == request.nivelNovo();

        NivelAcesso nivelAntigo = funcionario.getNivelAcesso();
        if (!inalterado) {
            Funcionario admin = funcionarioRepository.findById(principal.idFuncionario())
                    .orElseThrow(() -> new ForbiddenException("Funcionário responsável não encontrado"));
            funcionario.setNivelAcesso(request.nivelNovo());
            funcionarioRepository.save(funcionario);

            HistoricoNivel historico = new HistoricoNivel();
            historico.setFuncionario(funcionario);
            historico.setNivelAntigo(nivelAntigo);
            historico.setNivelNovo(request.nivelNovo());
            historico.setAdmin(admin);
            historico.setDataAlteracao(Instant.now());
            historicoRepository.save(historico);

            eventPublisher.publishNivelAlterado(new NivelAlteradoEvent(
                    funcionario.getId(), nivelAntigo, request.nivelNovo(), historico.getDataAlteracao()));
        }

        return new NivelAlteradoResponse(
                funcionario.getId(),
                nivelAntigo,
                request.nivelNovo(),
                inalterado ? Instant.now() : historicoData(funcionario));
    }

    @Transactional(readOnly = true)
    public FuncionarioHorasResponse totalHoras(Long id, RhPrincipal principal) {
        Funcionario funcionario = obter(id);
        if (!com.fablab.rh.dto.AutorizacaoHelper.ehAdminOuFuncionario(principal, funcionario)) {
            throw new ForbiddenException("Acesso apenas aos seus próprios dados");
        }

        List<RegistroPontoDiario> registros = pontoRepository.findByFuncionarioId(id);
        List<ApontamentoHoras> apontamentos = apontamentoRepository.findByFuncionarioId(id).stream()
                .filter(a -> a.getStatus() == StatusApontamento.PENDENTE
                        || a.getStatus() == StatusApontamento.VALIDADO)
                .toList();

        BigDecimal totalPresenca = registros.stream()
                .map(RegistroPontoDiario::getTotalHoras)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEncomenda = somar(apontamentos, a -> a.getTipo() == com.fablab.rh.entity.TipoApontamento.ENCOMENDA);
        BigDecimal totalProjeto = somar(apontamentos, a -> a.getTipo() == com.fablab.rh.entity.TipoApontamento.PROJETO);

        java.util.Map<LocalDate, List<ApontamentoHoras>> apontamentosPorData = apontamentos.stream()
                .collect(Collectors.groupingBy(ApontamentoHoras::getData));

        Set<LocalDate> datas = new TreeSet<>();
        datas.addAll(apontamentosPorData.keySet());
        registros.stream().map(RegistroPontoDiario::getData).forEach(datas::add);

        List<FuncionarioHorasResponse.IncoerenciaHoras> incoerencias = new ArrayList<>();
        for (LocalDate data : datas) {
            BigDecimal presenca = registros.stream()
                    .filter(r -> r.getData().equals(data) && r.getTotalHoras() != null)
                    .map(RegistroPontoDiario::getTotalHoras)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal apontado = apontamentosPorData.getOrDefault(data, List.of()).stream()
                    .map(ApontamentoHoras::getHorasTrabalhadas)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (presenca.compareTo(apontado) < 0) {
                incoerencias.add(new FuncionarioHorasResponse.IncoerenciaHoras(data, presenca, apontado));
            }
        }

        return new FuncionarioHorasResponse(id, totalPresenca, totalEncomenda, totalProjeto, incoerencias);
    }

    private BigDecimal somar(List<ApontamentoHoras> apontamentos,
                             Function<ApontamentoHoras, Boolean> filtro) {
        return apontamentos.stream()
                .filter(filtro::apply)
                .map(ApontamentoHoras::getHorasTrabalhadas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Data da última alteração registrada (utilizada quando o nível não muda). */
    private Instant historicoData(Funcionario funcionario) {
        return historicoRepository.findByFuncionarioId(funcionario.getId()).stream()
                .map(HistoricoNivel::getDataAlteracao)
                .max(Instant::compareTo)
                .orElse(Instant.now());
    }

    public Funcionario obter(Long id) {
        return funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado: " + id));
    }
}