package com.fablab.producao.service;

import com.fablab.producao.dto.Inspecao5SRequest;
import com.fablab.producao.dto.Inspecao5SResponse;
import com.fablab.producao.dto.ItemInspecaoRequest;
import com.fablab.producao.dto.ItemInspecaoResponse;
import com.fablab.producao.entity.Inspecao5S;
import com.fablab.producao.entity.ItemInspecao5S;
import com.fablab.producao.entity.Setor;
import com.fablab.producao.entity.SetorChecklist;
import com.fablab.producao.entity.SetorResponsavel;
import com.fablab.producao.entity.StatusInspecao;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.Inspecao5SRepository;
import com.fablab.producao.repository.SetorChecklistRepository;
import com.fablab.producao.repository.SetorResponsavelRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inspeções 5S: cada item do checklist do setor é avaliado; se houver qualquer
 * não conformidade, a inspeção é marcada como {@code NAO_CONFORME} e uma
 * advertência é aplicada ao responsável ativo do setor.
 */
@Service
public class Inspecao5SService {

    private final Inspecao5SRepository inspecaoRepository;
    private final SetorChecklistRepository checklistRepository;
    private final SetorResponsavelRepository responsavelRepository;
    private final SetorService setorService;
    private final AdvertenciaService advertenciaService;

    public Inspecao5SService(Inspecao5SRepository inspecaoRepository,
                             SetorChecklistRepository checklistRepository,
                             SetorResponsavelRepository responsavelRepository,
                             SetorService setorService,
                             AdvertenciaService advertenciaService) {
        this.inspecaoRepository = inspecaoRepository;
        this.checklistRepository = checklistRepository;
        this.responsavelRepository = responsavelRepository;
        this.setorService = setorService;
        this.advertenciaService = advertenciaService;
    }

    @Transactional(readOnly = true)
    public List<Inspecao5SResponse> listar(Long idSetor) {
        List<Inspecao5S> inspecoes = idSetor == null
                ? inspecaoRepository.findAll()
                : inspecaoRepository.findBySetor_IdSetorOrderByDataInspecaoDesc(idSetor);
        return inspecoes.stream().map(i -> Inspecao5SResponse.of(i, mapearItens(i))).toList();
    }

    @Transactional(readOnly = true)
    public Inspecao5SResponse buscar(Long id) {
        Inspecao5S inspecao = obter(id);
        return Inspecao5SResponse.of(inspecao, mapearItens(inspecao));
    }

    @Transactional
    public Inspecao5SResponse registrar(Inspecao5SRequest request) {
        Setor setor = setorService.obter(request.idSetor());
        Inspecao5S inspecao = new Inspecao5S();
        inspecao.setSetor(setor);
        inspecao.setIdInspetor(request.idInspetor());
        inspecao.setDataInspecao(request.dataInspecao());
        inspecao.setTurno(request.turno());
        inspecao.setObservacoes(request.observacoes());

        for (ItemInspecaoRequest itemRequest : request.itens()) {
            SetorChecklist checklist = checklistRepository.findById(itemRequest.idChecklist())
                    .orElseThrow(() -> new ResourceNotFoundException("Item de checklist", itemRequest.idChecklist()));
            if (!checklist.getSetor().getIdSetor().equals(setor.getIdSetor())) {
                throw new IllegalArgumentException("O item de checklist " + checklist.getIdChecklist()
                        + " não pertence ao setor " + setor.getIdSetor());
            }
            ItemInspecao5S item = new ItemInspecao5S();
            item.setChecklist(checklist);
            item.setConforme(itemRequest.conforme());
            item.setObservacao(itemRequest.observacao());
            inspecao.adicionarItem(item);
        }
        inspecao.recalcularStatus();
        Inspecao5S salva = inspecaoRepository.save(inspecao);

        if (salva.getStatus() == StatusInspecao.NAO_CONFORME) {
            responsavelRepository.findFirstBySetor_IdSetorAndAtivoTrue(setor.getIdSetor())
                    .ifPresent(responsavel -> aplicarAdvertencia(responsavel, salva));
        }
        return Inspecao5SResponse.of(salva, mapearItens(salva));
    }

    @Transactional
    public void remover(Long id) {
        inspecaoRepository.delete(obter(id));
    }

    private void aplicarAdvertencia(SetorResponsavel responsavel, Inspecao5S inspecao) {
        String motivo = "Não conformidade 5S no setor " + inspecao.getSetor().getNome()
                + " em " + inspecao.getDataInspecao();
        advertenciaService.registrarAutomatica(responsavel.getIdFuncionario(), inspecao, motivo);
    }

    private List<ItemInspecaoResponse> mapearItens(Inspecao5S inspecao) {
        return inspecao.getItens().stream().map(ItemInspecaoResponse::from).toList();
    }

    private Inspecao5S obter(Long id) {
        return inspecaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspeção 5S", id));
    }
}