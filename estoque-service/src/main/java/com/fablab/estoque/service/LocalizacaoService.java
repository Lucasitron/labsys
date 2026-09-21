package com.fablab.estoque.service;

import com.fablab.estoque.dto.LocalizacaoRequest;
import com.fablab.estoque.dto.LocalizacaoResponse;
import com.fablab.estoque.entity.Localizacao;
import com.fablab.estoque.mapper.LocalizacaoMapper;
import com.fablab.estoque.repository.LocalizacaoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de localizações físicas dos itens.
 */
@Service
public class LocalizacaoService {

    private final LocalizacaoRepository localizacaoRepository;

    public LocalizacaoService(LocalizacaoRepository localizacaoRepository) {
        this.localizacaoRepository = localizacaoRepository;
    }

    @Transactional
    public LocalizacaoResponse criar(LocalizacaoRequest request) {
        Localizacao localizacao = new Localizacao();
        localizacao.setArmario(request.armario());
        localizacao.setPrateleira(request.prateleira());
        localizacao.setCaixa(request.caixa());
        localizacao.setDescricao(request.descricao());
        return LocalizacaoMapper.toResponse(localizacaoRepository.save(localizacao));
    }

    @Transactional(readOnly = true)
    public List<LocalizacaoResponse> listar() {
        return localizacaoRepository.findAllByOrderByArmarioAscPrateleiraAscCaixaAsc().stream()
                .map(LocalizacaoMapper::toResponse)
                .toList();
    }
}