package com.fablab.producao.service;

import com.fablab.producao.dto.AuditoriaProjetoMesaRequest;
import com.fablab.producao.dto.AuditoriaProjetoMesaResponse;
import com.fablab.producao.dto.ProjetoMesaAbandonadoEvent;
import com.fablab.producao.dto.ProjetoMesaRequest;
import com.fablab.producao.dto.ProjetoMesaResponse;
import com.fablab.producao.entity.AuditoriaProjetoMesa;
import com.fablab.producao.entity.ProjetoMesa;
import com.fablab.producao.entity.StatusProjetoMesa;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.AuditoriaProjetoMesaRepository;
import com.fablab.producao.repository.ProjetoMesaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Projetos individuais de mesa, evolução, QR Code e auditorias de abandono. */
@Service
public class ProjetoMesaService {

    private final ProjetoMesaRepository projetoMesaRepository;
    private final AuditoriaProjetoMesaRepository auditoriaRepository;
    private final ProducaoEventPublisher eventPublisher;
    private final AcessoService acessoService;

    public ProjetoMesaService(ProjetoMesaRepository projetoMesaRepository,
                              AuditoriaProjetoMesaRepository auditoriaRepository,
                              ProducaoEventPublisher eventPublisher,
                              AcessoService acessoService) {
        this.projetoMesaRepository = projetoMesaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.eventPublisher = eventPublisher;
        this.acessoService = acessoService;
    }

    @Transactional(readOnly = true)
    public List<ProjetoMesaResponse> listar(StatusProjetoMesa status) {
        List<ProjetoMesa> projetos = status == null
                ? projetoMesaRepository.findAll()
                : projetoMesaRepository.findByStatus(status);
        return projetos.stream().map(ProjetoMesaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoMesaResponse buscar(Long id) {
        return ProjetoMesaResponse.from(obter(id));
    }

    @Transactional
    public ProjetoMesaResponse criar(ProjetoMesaRequest request) {
        ProjetoMesa projeto = new ProjetoMesa();
        aplicar(projeto, request);
        LocalDate hoje = LocalDate.now();
        projeto.setDataInicio(hoje);
        projeto.setDataUltimaEvolucao(hoje);
        projeto.setStatus(StatusProjetoMesa.ATIVO);
        projeto = projetoMesaRepository.save(projeto);
        projeto.setQrCodeTotem("fablab://projeto-mesa/" + projeto.getIdProjetoMesa());
        return ProjetoMesaResponse.from(projeto);
    }

    @Transactional
    public ProjetoMesaResponse atualizar(Long id, ProjetoMesaRequest request) {
        ProjetoMesa projeto = obter(id);
        aplicar(projeto, request);
        return ProjetoMesaResponse.from(projeto);
    }

    @Transactional
    public ProjetoMesaResponse registrarEvolucao(Long id) {
        ProjetoMesa projeto = obter(id);
        projeto.setDataUltimaEvolucao(LocalDate.now());
        if (projeto.getStatus() == StatusProjetoMesa.ABANDONADO) {
            projeto.setStatus(StatusProjetoMesa.ATIVO);
        }
        return ProjetoMesaResponse.from(projeto);
    }

    @Transactional
    public void remover(Long id) {
        projetoMesaRepository.delete(obter(id));
    }

    /** Retorna o conteúdo que deve ser codificado no QR Code do totem. */
    @Transactional(readOnly = true)
    public String conteudoQrCode(Long id) {
        return obter(id).getQrCodeTotem();
    }

    @Transactional
    public AuditoriaProjetoMesaResponse auditar(AuditoriaProjetoMesaRequest request) {
        ProjetoMesa projeto = obter(request.idProjetoMesa());
        AuditoriaProjetoMesa auditoria = new AuditoriaProjetoMesa();
        auditoria.setProjetoMesa(projeto);
        auditoria.setDataAuditoria(LocalDate.now());
        auditoria.setResultado(request.resultado());
        auditoria.setAcaoTomada(request.acaoTomada());
        auditoria.setIdAdminResponsavel(acessoService.idUsuario());
        AuditoriaProjetoMesa salva = auditoriaRepository.save(auditoria);

        projeto.setStatus(request.resultado());
        projeto.setDataUltimaEvolucao(LocalDate.now());
        if (request.resultado() == StatusProjetoMesa.ABANDONADO) {
            eventPublisher.publicarProjetoMesaAbandonado(new ProjetoMesaAbandonadoEvent(
                    projeto.getIdProjetoMesa(), projeto.getIdFuncionario(), request.acaoTomada()));
        }
        return AuditoriaProjetoMesaResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<AuditoriaProjetoMesaResponse> listarAuditorias(Long idProjetoMesa) {
        obter(idProjetoMesa);
        return auditoriaRepository.findByProjetoMesa_IdProjetoMesa(idProjetoMesa)
                .stream().map(AuditoriaProjetoMesaResponse::from).toList();
    }

    /** Projetos ativos sem evolução desde a data informada. */
    @Transactional(readOnly = true)
    public List<ProjetoMesaResponse> projetosSemEvolucao(LocalDate desde) {
        return projetoMesaRepository
                .findByStatusAndDataUltimaEvolucaoBefore(StatusProjetoMesa.ATIVO, desde)
                .stream().map(ProjetoMesaResponse::from).toList();
    }

    ProjetoMesa obter(Long id) {
        return projetoMesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto de mesa", id));
    }

    private void aplicar(ProjetoMesa projeto, ProjetoMesaRequest request) {
        projeto.setIdFuncionario(request.idFuncionario());
        projeto.setIdMesa(request.idMesa());
        projeto.setNomeProjeto(request.nomeProjeto());
        projeto.setTipoProjeto(request.tipoProjeto());
        projeto.setPrazoExecucao(request.prazoExecucao());
    }
}