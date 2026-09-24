package com.fablab.rh.service;

import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.mapper.PessoaMapper;
import com.fablab.rh.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão das pessoas do Fab Lab (cadastro, consulta e atualização).
 */
@Service
public class PessoaService {

    private final PessoaRepository pessoaRepository;

    public PessoaService(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional
    public PessoaResponse cadastrar(PessoaRequest request) {
        validarMatriculaDisponivel(request.matricula(), null);
        Pessoa pessoa = PessoaMapper.toEntity(request);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
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
        PessoaMapper.update(pessoa, request);
        return PessoaMapper.toResponse(pessoaRepository.save(pessoa));
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

    /** Usuários não admin só podem acessar/exibir seus próprios dados. */
    private void validarAcesso(Pessoa pessoa, RhPrincipal principal) {
        if (principal != null && !principal.isAdmin()
                && principal.idPessoa() != null
                && !principal.idPessoa().equals(pessoa.getId())) {
            throw new ForbiddenException("Acesso apenas aos seus próprios dados");
        }
    }
}