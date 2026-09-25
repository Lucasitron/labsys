package com.fablab.rh.service;

import com.fablab.rh.dto.CpfUtil;
import com.fablab.rh.dto.FacetaResponse;
import com.fablab.rh.dto.FiltrosPessoasResponse;
import com.fablab.rh.dto.HorasMesResponse;
import com.fablab.rh.dto.HorasPorStatusResponse;
import com.fablab.rh.dto.HistoricoNivelResponse;
import com.fablab.rh.dto.PaginacaoResponse;
import com.fablab.rh.dto.PendenciasResponse;
import com.fablab.rh.dto.PessoaDetalheResponse;
import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.dto.PessoasPaginaResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.TreinamentosResumoResponse;
import com.fablab.rh.entity.AvaliacaoTreinamento;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.exception.ConflitoException;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.PessoaMapper;
import com.fablab.rh.mapper.TreinamentoMapper;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.AvaliacaoTreinamentoRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HistoricoNivelRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.ProcessoSeletivoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão das pessoas do Fab Lab (cadastro, consulta e atualização).
 */
@Service
public class PessoaService {

    private static final int PAGE_SIZE_MAX = 100;

    private final PessoaRepository pessoaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final ApontamentoHorasRepository apontamentoRepository;
    private final AvaliacaoTreinamentoRepository avaliacaoRepository;
    private final HistoricoNivelRepository historicoRepository;
    private final ProcessoSeletivoRepository processoRepository;

    public PessoaService(PessoaRepository pessoaRepository,
                         FuncionarioRepository funcionarioRepository,
                         ApontamentoHorasRepository apontamentoRepository,
                         AvaliacaoTreinamentoRepository avaliacaoRepository,
                         HistoricoNivelRepository historicoRepository,
                         ProcessoSeletivoRepository processoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.apontamentoRepository = apontamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.historicoRepository = historicoRepository;
        this.processoRepository = processoRepository;
    }

    @Transactional
    public PessoaResponse cadastrar(PessoaRequest request) {
        validarMatriculaDisponivel(request.matricula(), null);
        String cpf = validarCpf(request.cpf(), null);
        Pessoa pessoa = PessoaMapper.toEntity(request);
        pessoa.setCpf(cpf);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
    }

    /**
     * Lista paginada com busca e filtros. Recrutando não acessa o módulo;
     * Estagiário enxerga apenas o próprio registro; demais níveis veem todos.
     */
    @Transactional(readOnly = true)
    public PessoasPaginaResponse listar(String busca, String departamento, NivelAcesso nivel,
                                        PessoaStatus status, int page, int pageSize,
                                        RhPrincipal principal) {
        if (principal != null && principal.nivel() == NivelAcesso.RECRUTANDO) {
            throw new ForbiddenException("Acesso negado ao módulo de pessoas");
        }
        String buscaNormalizada = busca == null || busca.isBlank() ? null : busca.trim();
        String departamentoNormalizado =
                departamento == null || departamento.isBlank() ? null : departamento.trim();
        int pagina = Math.max(page, 1);
        int tamanho = Math.min(Math.max(pageSize, 1), PAGE_SIZE_MAX);

        if (principal != null && !principal.isAdmin() && principal.nivel() == NivelAcesso.ESTAGIARIO) {
            return paginaDoProprioRegistro(buscaNormalizada, departamentoNormalizado, nivel, status,
                    pagina, tamanho, principal);
        }

        Page<Pessoa> resultado = pessoaRepository.buscarComFiltros(
                buscaNormalizada, status, nivel, departamentoNormalizado,
                PageRequest.of(pagina - 1, tamanho));
        List<PessoaResponse> pessoas = resultado.getContent().stream()
                .map(PessoaMapper::toResponse)
                .toList();
        return new PessoasPaginaResponse(
                pessoas,
                new PaginacaoResponse(pagina, tamanho, resultado.getTotalElements(),
                        resultado.getTotalPages()),
                facetas(buscaNormalizada));
    }

    /** Página contendo apenas o próprio registro do estagiário (quando casa com os filtros). */
    private PessoasPaginaResponse paginaDoProprioRegistro(String busca, String departamento,
                                                          NivelAcesso nivel, PessoaStatus status,
                                                          int pagina, int tamanho,
                                                          RhPrincipal principal) {
        Pessoa propria = principal.idPessoa() == null ? null
                : pessoaRepository.findById(principal.idPessoa()).orElse(null);
        List<PessoaResponse> pessoas = new ArrayList<>();
        List<FacetaResponse> statuses = new ArrayList<>();
        List<FacetaResponse> niveis = new ArrayList<>();
        List<FacetaResponse> setores = new ArrayList<>();
        if (propria != null && casaComFiltros(propria, busca, departamento, nivel, status, principal)) {
            pessoas.add(PessoaMapper.toResponse(propria));
            statuses.add(faceta(propria.getStatus().name().toLowerCase(), rotuloStatus(propria.getStatus()), 1));
            funcionarioRepository.findByPessoaId(propria.getId()).ifPresent(func -> {
                niveis.add(faceta(func.getNivelAcesso().name().toLowerCase(),
                        func.getNivelAcesso().getLabel(), 1));
                if (func.getDepartamento() != null) {
                    setores.add(faceta(func.getDepartamento(), func.getDepartamento(), 1));
                }
            });
        }
        int totalPages = pessoas.isEmpty() ? 0 : 1;
        List<PessoaResponse> paginaItens = pagina == 1 ? pessoas : List.of();
        return new PessoasPaginaResponse(paginaItens,
                new PaginacaoResponse(pagina, tamanho, pessoas.size(), totalPages),
                new FiltrosPessoasResponse(statuses, niveis, setores));
    }

    private boolean casaComFiltros(Pessoa pessoa, String busca, String departamento,
                                   NivelAcesso nivel, PessoaStatus status, RhPrincipal principal) {
        if (status != null && pessoa.getStatus() != status) {
            return false;
        }
        if (busca != null) {
            String termo = busca.toLowerCase();
            boolean casa = pessoa.getNomeCompleto().toLowerCase().contains(termo)
                    || pessoa.getMatricula().toLowerCase().contains(termo)
                    || (pessoa.getContato() != null && pessoa.getContato().toLowerCase().contains(termo));
            if (!casa) {
                return false;
            }
        }
        if (nivel != null || departamento != null) {
            Funcionario func = funcionarioRepository.findByPessoaId(pessoa.getId()).orElse(null);
            if (func == null) {
                return false;
            }
            if (nivel != null && func.getNivelAcesso() != nivel) {
                return false;
            }
            if (departamento != null && (func.getDepartamento() == null
                    || !func.getDepartamento().equalsIgnoreCase(departamento))) {
                return false;
            }
        }
        return true;
    }

    private FiltrosPessoasResponse facetas(String busca) {
        List<FacetaResponse> statuses = new ArrayList<>();
        for (Object[] linha : pessoaRepository.contarPorStatus(busca)) {
            PessoaStatus status = (PessoaStatus) linha[0];
            statuses.add(faceta(status.name().toLowerCase(), rotuloStatus(status),
                    (Long) linha[1]));
        }
        List<FacetaResponse> niveis = new ArrayList<>();
        for (Object[] linha : funcionarioRepository.contarPorNivel()) {
            NivelAcesso nivel = (NivelAcesso) linha[0];
            niveis.add(faceta(nivel.name().toLowerCase(), nivel.getLabel(), (Long) linha[1]));
        }
        List<FacetaResponse> setores = new ArrayList<>();
        for (Object[] linha : funcionarioRepository.contarPorDepartamento()) {
            String departamento = (String) linha[0];
            setores.add(faceta(departamento, departamento, (Long) linha[1]));
        }
        return new FiltrosPessoasResponse(statuses, niveis, setores);
    }

    private FacetaResponse faceta(String id, String label, long count) {
        return new FacetaResponse(id, label, count);
    }

    private String rotuloStatus(PessoaStatus status) {
        return switch (status) {
            case ATIVO -> "Ativo";
            case INATIVO -> "Inativo";
            case RECRUTANDO -> "Recrutando";
        };
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscar(Long id, RhPrincipal principal) {
        Pessoa pessoa = obter(id);
        validarAcesso(pessoa, principal);
        return PessoaMapper.toResponse(pessoa);
    }

    @Transactional
    public PessoaResponse atualizar(Long id, PessoaRequest request, RhPrincipal principal) {
        Pessoa pessoa = obter(id);
        validarAcesso(pessoa, principal);
        validarMatriculaDisponivel(request.matricula(), id);
        String cpf = request.cpf() == null ? pessoa.getCpf() : validarCpf(request.cpf(), id);
        PessoaMapper.update(pessoa, request);
        pessoa.setCpf(cpf);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
    }

    /**
     * Detalhe agregado (abas: visão geral, horas, treinamentos e histórico de
     * nível). Sem vínculo de funcionário, os blocos agregados degradam para
     * vazio.
     */
    @Transactional(readOnly = true)
    public PessoaDetalheResponse detalhe(Long id, RhPrincipal principal) {
        Pessoa pessoa = obter(id);
        validarAcesso(pessoa, principal);

        Funcionario funcionario = funcionarioRepository.findByPessoaId(id).orElse(null);
        if (funcionario == null) {
            return new PessoaDetalheResponse(
                    PessoaMapper.toResponse(pessoa),
                    null, null, null,
                    new HorasMesResponse(BigDecimal.ZERO, BigDecimal.ZERO),
                    new TreinamentosResumoResponse(0, null),
                    new PendenciasResponse(0),
                    new HorasPorStatusResponse(0, 0, 0),
                    List.of(), List.of());
        }

        Long idFuncionario = funcionario.getId();
        LocalDate hoje = LocalDate.now();
        YearMonth mesAnterior = YearMonth.from(hoje).minusMonths(1);
        BigDecimal atual = somarValidadasNoPeriodo(idFuncionario,
                hoje.withDayOfMonth(1), hoje);
        BigDecimal anterior = somarValidadasNoPeriodo(idFuncionario,
                mesAnterior.atDay(1), mesAnterior.atEndOfMonth());

        List<AvaliacaoTreinamento> avaliacoes = avaliacaoRepository.findByFuncionarioId(idFuncionario);
        BigDecimal media = avaliacoes.isEmpty() ? null
                : avaliacoes.stream().map(AvaliacaoTreinamento::getNota)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(avaliacoes.size()), 2, RoundingMode.HALF_UP);

        List<HistoricoNivelResponse> historico = historicoRepository.findByFuncionarioId(idFuncionario)
                .stream().map(h -> new HistoricoNivelResponse(
                        h.getNivelAntigo(), h.getNivelAntigo().getLabel(),
                        h.getNivelNovo(), h.getNivelNovo().getLabel(),
                        h.getAdmin().getPessoa().getNomeCompleto(),
                        h.getDataAlteracao()))
                .toList();

        return new PessoaDetalheResponse(
                PessoaMapper.toResponse(pessoa),
                idFuncionario,
                funcionario.getNivelAcesso(),
                funcionario.getDepartamento(),
                new HorasMesResponse(atual, anterior),
                new TreinamentosResumoResponse(avaliacoes.size(), media),
                new PendenciasResponse(apontamentoRepository.countByFuncionarioIdAndStatus(
                        idFuncionario, StatusApontamento.PENDENTE)),
                new HorasPorStatusResponse(
                        apontamentoRepository.countByFuncionarioIdAndStatus(
                                idFuncionario, StatusApontamento.PENDENTE),
                        apontamentoRepository.countByFuncionarioIdAndStatus(
                                idFuncionario, StatusApontamento.VALIDADO),
                        apontamentoRepository.countByFuncionarioIdAndStatus(
                                idFuncionario, StatusApontamento.REJEITADO)),
                avaliacoes.stream().map(TreinamentoMapper::toAvaliacaoResponse).toList(),
                historico);
    }

    /**
     * Exclui a pessoa (Admin). Recusa quando há vínculo de funcionário ou
     * processo seletivo, preservando a integridade referencial.
     */
    @Transactional
    public void excluir(Long id) {
        Pessoa pessoa = obter(id);
        if (funcionarioRepository.existsByPessoaId(id)) {
            throw new ConflitoException(
                    "Pessoa possui vínculo de funcionário e não pode ser excluída: " + id);
        }
        if (processoRepository.existsByCandidatoId(id)) {
            throw new ConflitoException(
                    "Pessoa possui processo seletivo e não pode ser excluída: " + id);
        }
        pessoaRepository.delete(pessoa);
    }

    private BigDecimal somarValidadasNoPeriodo(Long idFuncionario, LocalDate inicio, LocalDate fim) {
        BigDecimal encomenda = apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(
                idFuncionario, TipoApontamento.ENCOMENDA, inicio, fim);
        BigDecimal projeto = apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(
                idFuncionario, TipoApontamento.PROJETO, inicio, fim);
        BigDecimal totalEncomenda = encomenda == null ? BigDecimal.ZERO : encomenda;
        BigDecimal totalProjeto = projeto == null ? BigDecimal.ZERO : projeto;
        return totalEncomenda.add(totalProjeto);
    }

    public Pessoa obter(Long id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + id));
    }

    private void validarMatriculaDisponivel(String matricula, Long idAtual) {
        if (pessoaRepository.existsByMatricula(matricula)) {
            Pessoa existente = pessoaRepository.findByMatricula(matricula).orElse(null);
            if (existente == null || !existente.getId().equals(idAtual)) {
                throw new IllegalArgumentException("Matrícula já cadastrada: " + matricula);
            }
        }
    }

    /** Valida e normaliza o CPF (nulo quando ausente); rejeita inválido ou duplicado. */
    private String validarCpf(String cpf, Long idAtual) {
        String normalizado = CpfUtil.normalizar(cpf);
        if (normalizado == null) {
            return null;
        }
        if (!CpfUtil.valido(normalizado)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        if (pessoaRepository.existsByCpf(normalizado)) {
            Pessoa existente = pessoaRepository.findByCpf(normalizado).orElse(null);
            if (existente == null || !existente.getId().equals(idAtual)) {
                throw new IllegalArgumentException("CPF já cadastrado");
            }
        }
        return normalizado;
    }

    /**
     * Admin, Bolsista e Voluntário veem todos os registros; Estagiário,
     * Recrutando ou principal sem nível veem apenas o próprio registro.
     */
    private void validarAcesso(Pessoa pessoa, RhPrincipal principal) {
        if (principal == null || principal.isAdmin()
                || principal.nivel() == NivelAcesso.BOLSISTA
                || principal.nivel() == NivelAcesso.VOLUNTARIO) {
            return;
        }
        if (principal.idPessoa() == null || !principal.idPessoa().equals(pessoa.getId())) {
            throw new ForbiddenException("Acesso apenas aos seus próprios dados");
        }
    }
}