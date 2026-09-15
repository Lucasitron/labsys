package com.fablab.rh.service;

import com.fablab.rh.dto.ProcessoSeletivoRequest;
import com.fablab.rh.dto.ProcessoSeletivoResponse;
import com.fablab.rh.dto.ProcessoSeletivoStatusRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.ProcessoSeletivo;
import com.fablab.rh.entity.StatusProcesso;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.ProcessoSeletivoMapper;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.PessoaRepository;
import com.fablab.rh.repository.ProcessoSeletivoRepository;
import com.fablab.rh.repository.TutorRepository;
import java.time.LocalDate;
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

    public ProcessoSeletivoService(ProcessoSeletivoRepository processoRepository,
                                   PessoaRepository pessoaRepository,
                                   FuncionarioRepository funcionarioRepository,
                                   TutorRepository tutorRepository) {
        this.processoRepository = processoRepository;
        this.pessoaRepository = pessoaRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.tutorRepository = tutorRepository;
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
        if (principal == null || (!principal.isAdmin() && !ehTutor(principal))) {
            throw new ForbiddenException("Apenas Admin ou tutor podem atualizar o processo seletivo");
        }

        processo.setStatusProcesso(request.statusProcesso());
        if (request.resultadoFinal() != null) {
            processo.setResultadoFinal(request.resultadoFinal());
        }
        return ProcessoSeletivoMapper.toResponse(processoRepository.save(processo));
    }

    private Funcionario resolverTutor(Long idTutor, RhPrincipal principal) {
        if (principal != null && principal.isAdmin()) {
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