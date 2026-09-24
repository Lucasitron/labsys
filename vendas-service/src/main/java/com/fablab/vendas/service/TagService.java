package com.fablab.vendas.service;

import com.fablab.vendas.dto.TagDtos.TagListaResponse;
import com.fablab.vendas.dto.TagDtos.TagRequest;
import com.fablab.vendas.dto.TagDtos.TagResponse;
import com.fablab.vendas.entity.TagCliente;
import com.fablab.vendas.exception.ConflitoException;
import com.fablab.vendas.repository.TagClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e listagem de tags de clientes. */
@Service
public class TagService {

    private final TagClienteRepository tagRepository;

    public TagService(TagClienteRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public TagResponse criar(TagRequest request) {
        if (tagRepository.existsByNome(request.nome().trim())) {
            throw new ConflitoException("Tag já cadastrada");
        }
        TagCliente tag = new TagCliente();
        tag.setNome(request.nome().trim());
        tag.setCor(request.cor());
        tag = tagRepository.save(tag);
        return new TagResponse(tag.getId(), tag.getNome(), tag.getCor());
    }

    @Transactional(readOnly = true)
    public TagListaResponse listar() {
        return new TagListaResponse(tagRepository.findAll().stream()
                .map(t -> new TagResponse(t.getId(), t.getNome(), t.getCor())).toList());
    }
}
