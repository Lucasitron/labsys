package com.fablab.rh.mapper;

import com.fablab.rh.dto.FuncionarioRequest;
import com.fablab.rh.dto.FuncionarioResponse;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.Pessoa;

/**
 * Mapeia entidades {@code Funcionario} para DTOs.
 */
public final class FuncionarioMapper {

    private FuncionarioMapper() {
    }

    public static Funcionario toEntity(Pessoa pessoa, FuncionarioRequest request) {
        Funcionario funcionario = new Funcionario();
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(request.nivelOrDefault());
        funcionario.setDepartamento(request.departamento());
        return funcionario;
    }

    public static FuncionarioResponse toResponse(Funcionario funcionario) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getPessoa().getId(),
                funcionario.getPessoa().getNomeCompleto(),
                funcionario.getNivelAcesso(),
                funcionario.getDepartamento());
    }
}