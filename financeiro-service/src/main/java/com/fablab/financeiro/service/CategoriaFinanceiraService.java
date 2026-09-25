package com.fablab.financeiro.service;

import com.fablab.financeiro.dto.CategoriaDtos.CategoriaRequest;
import com.fablab.financeiro.dto.CategoriaDtos.CategoriaResponse;
import com.fablab.financeiro.entity.CategoriaFinanceira;
import com.fablab.financeiro.entity.TipoCategoria;
import com.fablab.financeiro.repository.CategoriaFinanceiraRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e listagem de categorias financeiras (receita/despesa). */
@Service
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository repository;

    public CategoriaFinanceiraService(CategoriaFinanceiraRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        CategoriaFinanceira entity = new CategoriaFinanceira();
        entity.setNome(request.nome().trim());
        entity.setTipo(request.tipo());
        entity.setDescricao(request.descricao());
        return CategoriaResponse.of(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(TipoCategoria tipo) {
        List<CategoriaFinanceira> entities = tipo == null ? repository.findAll() : repository.findByTipo(tipo);
        return entities.stream().map(CategoriaResponse::of).toList();
    }
}
