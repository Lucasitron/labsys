package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CategoriaFinanceiraRequest;
import com.fablab.financeiro.dto.CategoriaFinanceiraResponse;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoriaFinanceira;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de categorias financeiras (receitas e despesas).
 */
@Service
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository categoriaRepository;

    public CategoriaFinanceiraService(CategoriaFinanceiraRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public CategoriaFinanceiraResponse criar(CategoriaFinanceiraRequest request) {
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new IllegalArgumentException("Já existe categoria com o nome informado");
        }
        CategoriaFinanceira categoria = new CategoriaFinanceira();
        categoria.setNome(request.nome());
        categoria.setTipo(request.tipo());
        categoria.setDescricao(request.descricao());
        return CategoriaFinanceiraResponse.of(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaFinanceiraResponse> listar(TipoCategoriaFinanceira tipo) {
        List<CategoriaFinanceira> categorias = tipo == null
                ? categoriaRepository.findAll()
                : categoriaRepository.findByTipo(tipo);
        return categorias.stream().map(CategoriaFinanceiraResponse::of).toList();
    }
}