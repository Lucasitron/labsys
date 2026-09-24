package com.fablab.rh.service;

import com.fablab.rh.dto.AvaliarCandidatoRequest;
import com.fablab.rh.dto.CandidatoGrupoResponse;
import com.fablab.rh.dto.EstagioRequest;
import com.fablab.rh.dto.GrupoProcessoRequest;
import com.fablab.rh.dto.GrupoProcessoResponse;
import com.fablab.rh.dto.ProcessoSeletivoListaResponse;
import com.fablab.rh.dto.ProcessoSeletivoRequest;
import com.fablab.rh.dto.ProcessoSeletivoResponse;
import com.fablab.rh.dto.ProcessoSeletivoStatusRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.TotaisProcessoResponse;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.GrupoProcessoSeletivo;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.ProcessoSeletivo;
import com.fablab.rh.entity.StatusProcesso;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.ProcessoSeletivoMapper;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.GrupoProcessoSeletivoRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.ProcessoSeletivoRepository;
import com.fablab.rh.repository.TutorRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processo seletivo: criação do candidato (pessoa + funcionário "Recrutando")
 * e evolução do status.
 */
@Service
public class ProcessoSeletivoService {

    private final ProcessoSeletivoRepository processoRepository;
    private final PessoaRepository pessoaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final TutorRepository tutorRepository;
    private final GrupoProcessoSeletivoRepository grupoRepository;

    public ProcessoSeletivoService(ProcessoSeletivoRepository processoRepository,
                                   PessoaRepository pessoaRepository,
                                   FuncionarioRepository funcionarioRepository,
                                   TutorRepository tutorRepository,
                                   GrupoProcessoSeletivoRepository grupoRepository) {
        this.processoRepository = processoRepository;
        this.pessoaRepository = pessoaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.tutorRepository = tutorRepository;
        this.grupoRepository = grupoRepository;
    }

    @Transactional
    public ProcessoSeletivoResponse iniciar(ProcessoSeletivoRequest request, RhPrincipal principal) {
        if (pessoaRepository.existsByMatricula(request.matricula())) {
            throw new IllegalArgumentException("Matrícula já cadastrada: " + request.matricula());
        }

        Funcionario tutor = resolverTutor(request.idTutor(), principal);
        if (principal != null && !principal.isAdmin()
                && !tutor.getId().equals(principal.idFuncionario())) {
            throw new ForbiddenException("Tutor só pode se responsabilizar pelo próprio funcionário");
        }

        Pessoa candidato = new Pessoa();
        candidato.setNomeCompleto(request.nomeCompleto());
        candidato.setMatricula(request.matricula());
        candidato.setContato(request.contato());
        candidato.setTurno(request.turno());
        candidato.setStatus(PessoaStatus.RECRUTANDO);
        candidato = pessoaRepository.save(candidato);

        Funcionario funcionario = new Funcionario();
        funcionario.setPessoa(candidato);
        funcionario.setNivelAcesso(NivelAcesso.RECRUTANDO);
        funcionarioRepository.save(funcionario);

        ProcessoSeletivo processo = new ProcessoSeletivo();
        processo.setCandidato(candidato);
        processo.setTutor(tutor);
        processo.setStatusProcesso(StatusProcesso.INSCRITO);
        processo.setDataInscricao(LocalDate.now());
        return ProcessoSeletivoMapper.toResponse(processoRepository.save(processo));
    }

    @Transactional
    public ProcessoSeletivoResponse atualizarStatus(Long id, ProcessoSeletivoStatusRequest request,
                                                    RhPrincipal principal) {
        ProcessoSeletivo processo = obter(id);
        exigirTutorOuAdmin(processo, principal);

        processo.setStatusProcesso(request.statusProcesso());
        if (request.resultadoFinal() != null) {
            processo.setResultadoFinal(request.resultadoFinal());
        }
        return ProcessoSeletivoMapper.toResponse(processoRepository.save(processo));
    }

    /**
     * Kanban por grupos com etapa individual dos candidatos. Candidatos sem
     * grupo aparecem em "Sem grupo".
     */
    @Transactional(readOnly = true)
    public ProcessoSeletivoListaResponse listar(StatusProcesso estagio, RhPrincipal principal) {
        exigirTutorOuAdmin(null, principal);

        List<GrupoProcessoResponse> resposta = new ArrayList<>();
        for (GrupoProcessoSeletivo grupo : grupoRepository.findAll()) {
            List<ProcessoSeletivo> membros = filtrarPorEstagio(
                    processoRepository.findByGrupoId(grupo.getId()), estagio);
            if (estagio == null || !membros.isEmpty()) {
                resposta.add(paraGrupo(grupo, membros));
            }
        }
        List<ProcessoSeletivo> semGrupo = filtrarPorEstagio(
                processoRepository.findByGrupoIsNull(), estagio);
        if (!semGrupo.isEmpty()) {
            resposta.add(new GrupoProcessoResponse(
                    null, "Sem grupo", etapaPredominante(semGrupo), semGrupo.size(),
                    null, null, paraCandidatos(semGrupo)));
        }

        long emAvaliacao = processoRepository.countByStatusProcesso(StatusProcesso.EM_TRIAGEM)
                + processoRepository.countByStatusProcesso(StatusProcesso.ENTREVISTA);
        return new ProcessoSeletivoListaResponse(resposta, new TotaisProcessoResponse(
                processoRepository.count(),
                grupoRepository.count(),
                emAvaliacao,
                processoRepository.countByStatusProcesso(StatusProcesso.APROVADO)));
    }

    /** Cria um grupo reunindo candidatos com processo aberto. */
    @Transactional
    public GrupoProcessoResponse criarGrupo(GrupoProcessoRequest request, RhPrincipal principal) {
        exigirTutorOuAdmin(null, principal);
        Funcionario lider = funcionarioRepository.findById(request.idLider())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Líder não encontrado: " + request.idLider()));
        if (tutorRepository.findByFuncionarioId(lider.getId()).isEmpty()) {
            throw new IllegalArgumentException("Líder do grupo deve ser tutor: " + request.idLider());
        }

        GrupoProcessoSeletivo grupo = new GrupoProcessoSeletivo();
        grupo.setNome(request.nome());
        grupo.setLider(lider);
        grupo.setEtapa(StatusProcesso.INSCRITO);
        grupo = grupoRepository.save(grupo);

        if (request.membroIds() != null) {
            for (Long pessoaId : request.membroIds()) {
                ProcessoSeletivo processo = processoRepository.findByCandidatoId(pessoaId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Candidato sem processo seletivo: " + pessoaId));
                processo.setGrupo(grupo);
                processoRepository.save(processo);
            }
        }
        return paraGrupo(grupo, processoRepository.findByGrupoId(grupo.getId()));
    }

    /** Move a etapa do candidato (triagem → entrevista → aprovado/reprovado). */
    @Transactional
    public ProcessoSeletivoResponse moverEstagio(Long id, EstagioRequest request, RhPrincipal principal) {
        ProcessoSeletivo processo = obter(id);
        exigirTutorOuAdmin(processo, principal);
        processo.setStatusProcesso(request.etapa());
        processoRepository.save(processo);
        if (processo.getGrupo() != null) {
            GrupoProcessoSeletivo grupo = processo.getGrupo();
            grupo.setEtapa(etapaPredominante(
                    processoRepository.findByGrupoId(grupo.getId()), grupo.getEtapa()));
            grupoRepository.save(grupo);
        }
        return ProcessoSeletivoMapper.toResponse(processo);
    }

    /** Avaliação individual do candidato pelo tutor responsável. */
    @Transactional
    public ProcessoSeletivoResponse avaliar(Long id, Long pessoaId, AvaliarCandidatoRequest request,
                                            RhPrincipal principal) {
        ProcessoSeletivo processo = obter(id);
        if (!processo.getCandidato().getId().equals(pessoaId)) {
            throw new ResourceNotFoundException(
                    "Candidato " + pessoaId + " não pertence ao processo " + id);
        }
        exigirTutorOuAdmin(processo, principal);
        processo.setNota(request.nota());
        processo.setFeedback(request.feedback());
        return ProcessoSeletivoMapper.toResponse(processoRepository.save(processo));
    }

    /** Apenas Admin ou tutor do processo (ou qualquer tutor na listagem/criação). */
    private void exigirTutorOuAdmin(ProcessoSeletivo processo, RhPrincipal principal) {
        if (principal == null || (!principal.isAdmin() && !ehTutor(principal))) {
            throw new ForbiddenException("Apenas Admin ou tutor podem gerir o processo seletivo");
        }
        if (processo != null && !principal.isAdmin()
                && !processo.getTutor().getId().equals(principal.idFuncionario())) {
            throw new ForbiddenException("Apenas o tutor responsável pode alterar este processo");
        }
    }

    private List<ProcessoSeletivo> filtrarPorEstagio(List<ProcessoSeletivo> processos,
                                                      StatusProcesso estagio) {
        if (estagio == null) {
            return processos;
        }
        return processos.stream()
                .filter(p -> p.getStatusProcesso() == estagio)
                .toList();
    }

    private GrupoProcessoResponse paraGrupo(GrupoProcessoSeletivo grupo,
                                            List<ProcessoSeletivo> membros) {
        return new GrupoProcessoResponse(
                grupo.getId(),
                grupo.getNome(),
                etapaPredominante(membros, grupo.getEtapa()),
                membros.size(),
                grupo.getLider().getId(),
                grupo.getLider().getPessoa().getNomeCompleto(),
                paraCandidatos(membros));
    }

    private List<CandidatoGrupoResponse> paraCandidatos(List<ProcessoSeletivo> processos) {
        return processos.stream()
                .map(p -> new CandidatoGrupoResponse(
                        p.getCandidato().getId(),
                        p.getCandidato().getNomeCompleto(),
                        p.getStatusProcesso(),
                        p.getNota(),
                        p.getFeedback()))
                .toList();
    }

    private StatusProcesso etapaPredominante(List<ProcessoSeletivo> membros, StatusProcesso padrao) {
        if (membros.isEmpty()) {
            return padrao;
        }
        return membros.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ProcessoSeletivo::getStatusProcesso,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(padrao);
    }

    private StatusProcesso etapaPredominante(List<ProcessoSeletivo> membros) {
        return etapaPredominante(membros, StatusProcesso.INSCRITO);
    }

    private Funcionario resolverTutor(Long idTutor, RhPrincipal principal) {        if (principal != null && principal.isAdmin()) {
            return funcionarioRepository.findById(idTutor)
                    .orElseThrow(() -> new ResourceNotFoundException("Tutor não encontrado: " + idTutor));
        }
        if (principal == null || principal.idFuncionario() == null || !ehTutor(principal)) {
            throw new ForbiddenException("Apenas Admin ou tutor podem iniciar um processo seletivo");
        }
        return funcionarioRepository.findById(principal.idFuncionario())
                .orElseThrow(() -> new ForbiddenException("Funcionário não encontrado"));
    }

    private boolean ehTutor(RhPrincipal principal) {
        return principal.idFuncionario() != null
                && tutorRepository.findByFuncionarioId(principal.idFuncionario()).isPresent();
    }

    public ProcessoSeletivo obter(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processo seletivo não encontrado: " + id));
    }
}