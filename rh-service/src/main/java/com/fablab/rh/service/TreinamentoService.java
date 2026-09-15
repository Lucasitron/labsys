package com.fablab.rh.service;

import com.fablab.rh.dto.AvaliacaoRequest;
import com.fablab.rh.dto.AvaliacaoResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.TreinamentoRequest;
import com.fablab.rh.dto.TreinamentoResponse;
import com.fablab.rh.entity.AvaliacaoTreinamento;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.Treinamento;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.TreinamentoMapper;
import com.fablab.rh.repository.AvaliacaoTreinamentoRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.TreinamentoRepository;
import com.fablab.rh.repository.TutorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Módulo de treinamento (LMS): guias, documentações e avaliações simples.
 */
@Service
public class TreinamentoService {

    private final TreinamentoRepository treinamentoRepository;
    private final AvaliacaoTreinamentoRepository avaliacaoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final TutorRepository tutorRepository;

    public TreinamentoService(TreinamentoRepository treinamentoRepository,
                              AvaliacaoTreinamentoRepository avaliacaoRepository,
                              FuncionarioRepository funcionarioRepository,
                              TutorRepository tutorRepository) {
        this.treinamentoRepository = treinamentoRepository;
        this.avaliacaoRepository = avaliacaoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.tutorRepository = tutorRepository;
    }

    @Transactional
    public TreinamentoResponse criar(TreinamentoRequest request, RhPrincipal principal) {
        Funcionario tutor = resolverTutor(request, principal);
        Treinamento treinamento = TreinamentoMapper.toEntity(request, tutor);
        return TreinamentoMapper.toResponse(treinamentoRepository.save(treinamento));
    }

    @Transactional
    public AvaliacaoResponse avaliar(Long treinamentoId, AvaliacaoRequest request, RhPrincipal principal) {
        if (principal == null || (!principal.isAdmin() && !ehTutor(principal))) {
            throw new ForbiddenException("Apenas Admin ou tutor podem avaliar treinamentos");
        }
        Treinamento treinamento = obter(treinamentoId);
        Funcionario aluno = funcionarioRepository.findById(request.idFuncionario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Funcionário não encontrado: " + request.idFuncionario()));
        AvaliacaoTreinamento avaliacao = TreinamentoMapper.toAvaliacaoEntity(treinamento, aluno, request);
        return TreinamentoMapper.toAvaliacaoResponse(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarAvaliacoes(Long treinamentoId) {
        obter(treinamentoId);
        return avaliacaoRepository.findByTreinamentoId(treinamentoId).stream()
                .map(TreinamentoMapper::toAvaliacaoResponse)
                .toList();
    }

    private Funcionario resolverTutor(TreinamentoRequest request, RhPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Autenticação necessária");
        }
        if (principal.isAdmin()) {
            if (request.idTutor() == null) {
                throw new IllegalArgumentException("idTutor é obrigatório quando criado por Admin");
            }
            return funcionarioRepository.findById(request.idTutor())
                    .orElseThrow(() -> new ResourceNotFoundException("Tutor não encontrado: " + request.idTutor()));
        }
        if (!ehTutor(principal)) {
            throw new ForbiddenException("Apenas Admin ou tutor podem criar treinamentos");
        }
        return funcionarioRepository.findById(principal.idFuncionario())
                .orElseThrow(() -> new ForbiddenException("Funcionário não encontrado"));
    }

    private boolean ehTutor(RhPrincipal principal) {
        return principal.idFuncionario() != null
                && tutorRepository.findByFuncionarioId(principal.idFuncionario()).isPresent();
    }

    public Treinamento obter(Long id) {
        return treinamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treinamento não encontrado: " + id));
    }
}