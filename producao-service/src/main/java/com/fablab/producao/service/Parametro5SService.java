package com.fablab.producao.service;

import com.fablab.producao.dto.Parametro5SRequest;
import com.fablab.producao.dto.Parametro5SResponse;
import com.fablab.producao.entity.Parametro5S;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.Parametro5SRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parâmetros configuráveis do sistema 5S (somente ADMIN altera). */
@Service
public class Parametro5SService {

    private final Parametro5SRepository parametroRepository;

    public Parametro5SService(Parametro5SRepository parametroRepository) {
        this.parametroRepository = parametroRepository;
    }

    @Transactional(readOnly = true)
    public List<Parametro5SResponse> listar() {
        return parametroRepository.findAll().stream().map(Parametro5SResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Parametro5SResponse buscar(Long id) {
        return Parametro5SResponse.from(obter(id));
    }

    @Transactional
    public Parametro5SResponse atualizar(Long id, Parametro5SRequest request) {
        Parametro5S parametro = obter(id);
        parametro.setValor(request.valor());
        parametro.setDescricao(request.descricao());
        return Parametro5SResponse.from(parametro);
    }

    /** Lê um parâmetro como inteiro, com valor padrão quando ausente ou inválido. */
    @Transactional(readOnly = true)
    public int obterInteiro(String chave, int padrao) {
        return parametroRepository.findByChave(chave)
                .map(Parametro5S::getValor)
                .map(valor -> {
                    try {
                        return Integer.parseInt(valor.trim());
                    } catch (NumberFormatException ex) {
                        return padrao;
                    }
                })
                .orElse(padrao);
    }

    private Parametro5S obter(Long id) {
        return parametroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parâmetro 5S", id));
    }
}