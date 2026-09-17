package com.fablab.producao.service;

import com.fablab.producao.dto.ProjetoRequest;
import com.fablab.producao.dto.ProjetoResponse;
import com.fablab.producao.dto.ProjetoStatusRequest;
import com.fablab.producao.entity.Projeto;
import com.fablab.producao.entity.ProjetoStatus;
import com.fablab.producao.exception.ForbiddenException;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.ProjetoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio de projetos. */
@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final AcessoService acessoService;

    public ProjetoService(ProjetoRepository projetoRepository, AcessoService acessoService) {
        this.projetoRepository = projetoRepository;
        this.acessoService = acessoService;
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listar(ProjetoStatus status, Long idResponsavel) {
        List<Projeto> projetos;
        if (status != null) {
            projetos = projetoRepository.findByStatus(status);
        } else if (idResponsavel != null) {
            projetos = projetoRepository.findByIdResponsavel(idResponsavel);
        } else {
            projetos = projetoRepository.findAll();
        }
        return projetos.stream().map(ProjetoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscar(Long id) {
        return ProjetoResponse.from(obter(id));
    }

    @Transactional
    public ProjetoResponse criar(ProjetoRequest request) {
        if (!acessoService.isAdmin() && !acessoService.podeEditar(request.idResponsavel())) {
            throw new ForbiddenException("Só é possível criar projetos dos quais você é o responsável");
        }
        Projeto projeto = new Projeto();
        aplicar(projeto, request);
        projeto.setStatus(request.status() == null ? ProjetoStatus.PLANEJADO : request.status());
        projeto.setDataFimReal(null);
        return ProjetoResponse.from(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse atualizar(Long id, ProjetoRequest request) {
        Projeto projeto = obter(id);
        verificarResponsabilidade(projeto);
        aplicar(projeto, request);
        return ProjetoResponse.from(projeto);
    }

    @Transactional
    public ProjetoResponse alterarStatus(Long id, ProjetoStatusRequest request) {
        Projeto projeto = obter(id);
        verificarResponsabilidade(projeto);
        projeto.setStatus(request.status());
        if (request.status() == ProjetoStatus.CONCLUIDO) {
            projeto.setDataFimReal(request.dataFimReal() == null ? LocalDate.now() : request.dataFimReal());
        } else {
            projeto.setDataFimReal(request.dataFimReal());
        }
        return ProjetoResponse.from(projeto);
    }

    @Transactional
    public void remover(Long id) {
        Projeto projeto = obter(id);
        verificarResponsabilidade(projeto);
        projetoRepository.delete(projeto);
    }

    public Projeto obter(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", id));
    }

    private void verificarResponsabilidade(Projeto projeto) {
        if (!acessoService.podeEditar(projeto.getIdResponsavel())) {
            throw new ForbiddenException("Apenas o responsável pelo projeto ou um administrador pode editá-lo");
        }
    }

    private void aplicar(Projeto projeto, ProjetoRequest request) {
        projeto.setNome(request.nome());
        projeto.setDescricao(request.descricao());
        projeto.setDataInicio(request.dataInicio());
        projeto.setDataFimPrevista(request.dataFimPrevista());
        projeto.setIdResponsavel(request.idResponsavel());
    }
}