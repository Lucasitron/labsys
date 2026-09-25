package com.fablab.producao.service;

import com.fablab.producao.dto.HistoricoUsoResponse;
import com.fablab.producao.dto.MaquinaRequest;
import com.fablab.producao.dto.MaquinaResponse;
import com.fablab.producao.dto.MaquinaStatusRequest;
import com.fablab.producao.dto.UsoMaquinaFimRequest;
import com.fablab.producao.dto.UsoMaquinaRequest;
import com.fablab.producao.entity.HistoricoUsoMaquina;
import com.fablab.producao.entity.Maquina;
import com.fablab.producao.entity.MaquinaStatus;
import com.fablab.producao.exception.ResourceNotFoundException;
import com.fablab.producao.repository.HistoricoUsoMaquinaRepository;
import com.fablab.producao.repository.MaquinaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de negócio de máquinas e seus históricos de uso. */
@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final HistoricoUsoMaquinaRepository usoRepository;

    public MaquinaService(MaquinaRepository maquinaRepository,
                          HistoricoUsoMaquinaRepository usoRepository) {
        this.maquinaRepository = maquinaRepository;
        this.usoRepository = usoRepository;
    }

    @Transactional(readOnly = true)
    public List<MaquinaResponse> listar(MaquinaStatus status) {
        List<Maquina> maquinas = status == null ? maquinaRepository.findAll() : maquinaRepository.findByStatus(status);
        return maquinas.stream().map(MaquinaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MaquinaResponse buscar(Long id) {
        return MaquinaResponse.from(obter(id));
    }

    @Transactional
    public MaquinaResponse criar(MaquinaRequest request) {
        Maquina maquina = new Maquina();
        aplicar(maquina, request);
        if (maquina.getStatus() == null) {
            maquina.setStatus(MaquinaStatus.DISPONIVEL);
        }
        return MaquinaResponse.from(maquinaRepository.save(maquina));
    }

    @Transactional
    public MaquinaResponse atualizar(Long id, MaquinaRequest request) {
        Maquina maquina = obter(id);
        aplicar(maquina, request);
        if (maquina.getStatus() == null) {
            maquina.setStatus(MaquinaStatus.DISPONIVEL);
        }
        return MaquinaResponse.from(maquina);
    }

    @Transactional
    public MaquinaResponse alterarStatus(Long id, MaquinaStatusRequest request) {
        Maquina maquina = obter(id);
        maquina.setStatus(request.status());
        return MaquinaResponse.from(maquina);
    }

    @Transactional
    public void remover(Long id) {
        maquinaRepository.delete(obter(id));
    }

    @Transactional
    public HistoricoUsoResponse iniciarUso(Long idMaquina, UsoMaquinaRequest request) {
        Maquina maquina = obter(idMaquina);
        HistoricoUsoMaquina uso = new HistoricoUsoMaquina();
        uso.setMaquina(maquina);
        uso.setIdFuncionario(request.idFuncionario());
        uso.setDataInicio(request.dataInicio() == null ? LocalDateTime.now() : request.dataInicio());
        uso.setObservacao(request.observacao());
        maquina.setStatus(MaquinaStatus.EM_USO);
        return HistoricoUsoResponse.from(usoRepository.save(uso));
    }

    @Transactional
    public HistoricoUsoResponse encerrarUso(Long idMaquina, Long idUso, UsoMaquinaFimRequest request) {
        Maquina maquina = obter(idMaquina);
        HistoricoUsoMaquina uso = usoRepository.findById(idUso)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de uso", idUso));
        if (!uso.getMaquina().getIdMaquina().equals(idMaquina)) {
            throw new IllegalArgumentException("O uso informado não pertence à máquina " + idMaquina);
        }
        LocalDateTime fim = request.dataFim() == null ? LocalDateTime.now() : request.dataFim();
        if (fim.isBefore(uso.getDataInicio())) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início");
        }
        uso.setDataFim(fim);
        uso.setHorasUso(calcularHoras(uso.getDataInicio(), fim));
        if (request.observacao() != null) {
            uso.setObservacao(request.observacao());
        }
        maquina.setStatus(MaquinaStatus.DISPONIVEL);
        return HistoricoUsoResponse.from(uso);
    }

    @Transactional(readOnly = true)
    public List<HistoricoUsoResponse> historico(Long idMaquina) {
        obter(idMaquina);
        return usoRepository.findByMaquina_IdMaquinaOrderByDataInicioDesc(idMaquina)
                .stream().map(HistoricoUsoResponse::from).toList();
    }

    private BigDecimal calcularHoras(LocalDateTime inicio, LocalDateTime fim) {
        long minutos = Duration.between(inicio, fim).toMinutes();
        return BigDecimal.valueOf(minutos)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Maquina obter(Long id) {
        return maquinaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Máquina", id));
    }

    private void aplicar(Maquina maquina, MaquinaRequest request) {
        maquina.setNome(request.nome());
        maquina.setDescricao(request.descricao());
        maquina.setLocalizacao(request.localizacao());
        if (request.status() != null) {
            maquina.setStatus(request.status());
        }
    }
}