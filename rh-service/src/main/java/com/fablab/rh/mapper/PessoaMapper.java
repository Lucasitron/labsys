package com.fablab.rh.mapper;

import com.fablab.rh.dto.CpfUtil;
import com.fablab.rh.dto.PessoaRequest;
import com.fablab.rh.dto.PessoaResponse;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;

/**
 * Mapeia entidades {@code Pessoa} para DTOs e aplica atualizações.
 */
public final class PessoaMapper {

    private PessoaMapper() {
    }

    public static Pessoa toEntity(PessoaRequest request) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNomeCompleto(request.nomeCompleto());
        pessoa.setMatricula(request.matricula());
        pessoa.setDataAdmissao(request.dataAdmissao());
        pessoa.setContato(request.contato());
        pessoa.setTurno(request.turno());
        pessoa.setStatus(request.status() == null ? PessoaStatus.ATIVO : request.status());
        pessoa.setCpf(CpfUtil.normalizar(request.cpf()));
        return pessoa;
    }

    public static void update(Pessoa pessoa, PessoaRequest request) {
        pessoa.setNomeCompleto(request.nomeCompleto());
        pessoa.setMatricula(request.matricula());
        if (request.dataAdmissao() != null) {
            pessoa.setDataAdmissao(request.dataAdmissao());
        }
        if (request.contato() != null) {
            pessoa.setContato(request.contato());
        }
        if (request.turno() != null) {
            pessoa.setTurno(request.turno());
        }
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }
        if (request.cpf() != null) {
            pessoa.setCpf(CpfUtil.normalizar(request.cpf()));
        }
    }

    public static PessoaResponse toResponse(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNomeCompleto(),
                pessoa.getMatricula(),
                pessoa.getDataAdmissao(),
                pessoa.getContato(),
                pessoa.getTurno(),
                pessoa.getStatus(),
                CpfUtil.mascarar(pessoa.getCpf()));
    }
}