package com.fablab.estoque.service;

import com.fablab.estoque.dto.FornecedorRequest;
import com.fablab.estoque.dto.FornecedorResponse;
import com.fablab.estoque.mapper.FornecedorMapper;
import com.fablab.estoque.repository.FornecedorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de fornecedores.
 */
@Service
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;

    public FornecedorService(FornecedorRepository fornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
    }

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        return FornecedorMapper.toResponse(fornecedorRepository.save(FornecedorMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public List<FornecedorResponse> listar() {
        return fornecedorRepository.findAll().stream().map(FornecedorMapper::toResponse).toList();
    }
}