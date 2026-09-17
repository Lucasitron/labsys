package com.fablab.producao.service;

import com.fablab.producao.dto.ChecklistItemRequest;
import com.fablab.producao.dto.ChecklistItemResponse;
import com.fablab.producao.dto.MaterialSetorRequest;
import com.fablab.producao.dto.MaterialSetorResponse;
import com.fablab.producao.dto.ResponsavelSetorRequest;
import com.fablab.producao.dto.ResponsavelSetorResponse;
import com.fablab.producao.dto.SetorRequest;
import com.fablab.producao.dto.SetorResponse;
import com.fablab.producao.dto.SinalizacaoSetorRequest;
import com.fablab.producao.dto.SinalizacaoSetorResponse;
import com.fablab.producao.entity.Setor;
import com.fablab.producao.entity.SetorChecklist;
import com.fablab.producao.entity.SetorMaterial;
import com.fablab.producao.entity.SetorResponsavel;
import com.fablab.producao.entity.SetorSinalizacao;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.SetorChecklistRepository;
import com.fablab.producao.repository.SetorMaterialRepository;
import com.fablab.producao.repository.SetorRepository;
import com.fablab.producao.repository.SetorResponsavelRepository;
import com.fablab.producao.repository.SetorSinalizacaoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio dos setores 5S, seus materiais, sinalizações, checklists e responsáveis. */
@Service
public class SetorService {

    private final SetorRepository setorRepository;
    private final SetorMaterialRepository materialRepository;
    private final SetorSinalizacaoRepository sinalizacaoRepository;
    private final SetorChecklistRepository checklistRepository;
    private final SetorResponsavelRepository responsavelRepository;

    public SetorService(SetorRepository setorRepository,
                        SetorMaterialRepository materialRepository,
                        SetorSinalizacaoRepository sinalizacaoRepository,
                        SetorChecklistRepository checklistRepository,
                        SetorResponsavelRepository responsavelRepository) {
        this.setorRepository = setorRepository;
        this.materialRepository = materialRepository;
        this.sinalizacaoRepository = sinalizacaoRepository;
        this.checklistRepository = checklistRepository;
        this.responsavelRepository = responsavelRepository;
    }

    @Transactional(readOnly = true)
    public List<SetorResponse> listar(Boolean ativo) {
        List<Setor> setores = Boolean.TRUE.equals(ativo) ? setorRepository.findByAtivoTrue() : setorRepository.findAll();
        return setores.stream().map(this::detalhar).toList();
    }

    @Transactional(readOnly = true)
    public SetorResponse buscar(Long id) {
        return detalhar(obter(id));
    }

    @Transactional
    public SetorResponse criar(SetorRequest request) {
        Setor setor = new Setor();
        aplicar(setor, request);
        return detalhar(setorRepository.save(setor));
    }

    @Transactional
    public SetorResponse atualizar(Long id, SetorRequest request) {
        Setor setor = obter(id);
        aplicar(setor, request);
        return detalhar(setor);
    }

    @Transactional
    public void remover(Long id) {
        setorRepository.delete(obter(id));
    }

    @Transactional
    public MaterialSetorResponse adicionarMaterial(Long idSetor, MaterialSetorRequest request) {
        Setor setor = obter(idSetor);
        SetorMaterial material = new SetorMaterial();
        material.setSetor(setor);
        material.setDescricao(request.descricao());
        material.setQuantidade(request.quantidade());
        return MaterialSetorResponse.from(materialRepository.save(material));
    }

    @Transactional
    public void removerMaterial(Long idSetor, Long idMaterial) {
        SetorMaterial material = materialRepository.findById(idMaterial)
                .orElseThrow(() -> new ResourceNotFoundException("Material", idMaterial));
        validarPertenceAoSetor(material.getSetor().getIdSetor(), idSetor, "Material");
        materialRepository.delete(material);
    }

    @Transactional
    public SinalizacaoSetorResponse adicionarSinalizacao(Long idSetor, SinalizacaoSetorRequest request) {
        Setor setor = obter(idSetor);
        SetorSinalizacao sinalizacao = new SetorSinalizacao();
        sinalizacao.setSetor(setor);
        sinalizacao.setTexto(request.texto());
        return SinalizacaoSetorResponse.from(sinalizacaoRepository.save(sinalizacao));
    }

    @Transactional
    public void removerSinalizacao(Long idSetor, Long idSinalizacao) {
        SetorSinalizacao sinalizacao = sinalizacaoRepository.findById(idSinalizacao)
                .orElseThrow(() -> new ResourceNotFoundException("Sinalização", idSinalizacao));
        validarPertenceAoSetor(sinalizacao.getSetor().getIdSetor(), idSetor, "Sinalização");
        sinalizacaoRepository.delete(sinalizacao);
    }

    @Transactional
    public ChecklistItemResponse adicionarChecklist(Long idSetor, ChecklistItemRequest request) {
        Setor setor = obter(idSetor);
        SetorChecklist checklist = new SetorChecklist();
        checklist.setSetor(setor);
        checklist.setItem(request.item());
        checklist.setAtivo(request.ativo() == null || request.ativo());
        return ChecklistItemResponse.from(checklistRepository.save(checklist));
    }

    @Transactional
    public ChecklistItemResponse atualizarChecklist(Long idSetor, Long idChecklist, ChecklistItemRequest request) {
        SetorChecklist checklist = checklistRepository.findById(idChecklist)
                .orElseThrow(() -> new ResourceNotFoundException("Item de checklist", idChecklist));
        validarPertenceAoSetor(checklist.getSetor().getIdSetor(), idSetor, "Item de checklist");
        checklist.setItem(request.item());
        if (request.ativo() != null) {
            checklist.setAtivo(request.ativo());
        }
        return ChecklistItemResponse.from(checklist);
    }

    @Transactional
    public void removerChecklist(Long idSetor, Long idChecklist) {
        SetorChecklist checklist = checklistRepository.findById(idChecklist)
                .orElseThrow(() -> new ResourceNotFoundException("Item de checklist", idChecklist));
        validarPertenceAoSetor(checklist.getSetor().getIdSetor(), idSetor, "Item de checklist");
        checklistRepository.delete(checklist);
    }

    @Transactional
    public ResponsavelSetorResponse adicionarResponsavel(Long idSetor, ResponsavelSetorRequest request) {
        Setor setor = obter(idSetor);
        if (Boolean.TRUE.equals(request.ativo())) {
            responsavelRepository.findFirstBySetor_IdSetorAndAtivoTrue(idSetor)
                    .ifPresent(r -> r.setAtivo(false));
        }
        SetorResponsavel responsavel = new SetorResponsavel();
        responsavel.setSetor(setor);
        responsavel.setIdFuncionario(request.idFuncionario());
        responsavel.setDataInicio(request.dataInicio());
        responsavel.setDataFim(request.dataFim());
        responsavel.setAtivo(request.ativo() == null || request.ativo());
        return ResponsavelSetorResponse.from(responsavelRepository.save(responsavel));
    }

    @Transactional
    public void removerResponsavel(Long idSetor, Long idResponsavel) {
        SetorResponsavel responsavel = responsavelRepository.findById(idResponsavel)
                .orElseThrow(() -> new ResourceNotFoundException("Responsável", idResponsavel));
        validarPertenceAoSetor(responsavel.getSetor().getIdSetor(), idSetor, "Responsável");
        responsavelRepository.delete(responsavel);
    }

    /** Encerra o vínculo ativo do responsável de um setor. */
    @Transactional
    public List<ResponsavelSetorResponse> atualizarAtivos(Long idSetor) {
        LocalDate hoje = LocalDate.now();
        List<SetorResponsavel> responsaveis = responsavelRepository.findBySetor_IdSetor(idSetor);
        responsaveis.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAtivo()))
                .filter(r -> r.getDataFim() != null && !r.getDataFim().isAfter(hoje))
                .forEach(r -> r.setAtivo(false));
        return responsaveis.stream().map(ResponsavelSetorResponse::from).toList();
    }

    public Setor obter(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor", id));
    }

    private void validarPertenceAoSetor(Long idSetorDoRecurso, Long idSetorEsperado, String recurso) {
        if (!idSetorDoRecurso.equals(idSetorEsperado)) {
            throw new IllegalArgumentException(recurso + " não pertence ao setor " + idSetorEsperado);
        }
    }

    private SetorResponse detalhar(Setor setor) {
        Long id = setor.getIdSetor();
        return SetorResponse.of(
                setor,
                materialRepository.findBySetor_IdSetor(id).stream().map(MaterialSetorResponse::from).toList(),
                sinalizacaoRepository.findBySetor_IdSetor(id).stream().map(SinalizacaoSetorResponse::from).toList(),
                checklistRepository.findBySetor_IdSetor(id).stream().map(ChecklistItemResponse::from).toList(),
                responsavelRepository.findBySetor_IdSetor(id).stream().map(ResponsavelSetorResponse::from).toList());
    }

    private void aplicar(Setor setor, SetorRequest request) {
        setor.setNumero(request.numero());
        setor.setNome(request.nome());
        setor.setDescricao(request.descricao());
        setor.setObservacoes(request.observacoes());
        setor.setFotoCorretoUrl(request.fotoCorretoUrl());
        setor.setFotoIncorretoUrl(request.fotoIncorretoUrl());
        setor.setAtivo(request.ativo() == null || request.ativo());
    }
}