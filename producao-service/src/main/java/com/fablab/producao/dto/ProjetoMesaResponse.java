package com.fablab.producao.dto;

import com.fablab.producao.entity.ProjetoMesa;
import com.fablab.producao.entity.StatusProjetoMesa;
import java.time.LocalDate;

/** Representação de um projeto de mesa. */
public record ProjetoMesaResponse(
        Long idProjetoMesa,
        Long idFuncionario,
        Long idMesa,
        String nomeProjeto,
        String tipoProjeto,
        LocalDate prazoExecucao,
        LocalDate dataInicio,
        LocalDate dataUltimaEvolucao,
        StatusProjetoMesa status,
        String qrCodeTotem) {

    public static ProjetoMesaResponse from(ProjetoMesa projeto) {
        return new ProjetoMesaResponse(
                projeto.getIdProjetoMesa(),
                projeto.getIdFuncionario(),
                projeto.getIdMesa(),
                projeto.getNomeProjeto(),
                projeto.getTipoProjeto(),
                projeto.getPrazoExecucao(),
                projeto.getDataInicio(),
                projeto.getDataUltimaEvolucao(),
                projeto.getStatus(),
                projeto.getQrCodeTotem());
    }
}