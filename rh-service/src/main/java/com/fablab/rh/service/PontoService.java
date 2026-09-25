package com.fablab.rh.service;

import com.fablab.rh.dto.RfidAccessEvent;
import com.fablab.rh.entity.RegistroPontoDiario;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consome os eventos de acesso RFID do Auth &amp; Identity Service e consolida
 * o registro de ponto diário (entrada, saída e total de horas).
 */
@Service
public class PontoService {

    private static final Logger log = LoggerFactory.getLogger(PontoService.class);

    private final FuncionarioRepository funcionarioRepository;
    private final RegistroPontoDiarioRepository pontoRepository;

    public PontoService(FuncionarioRepository funcionarioRepository,
                        RegistroPontoDiarioRepository pontoRepository) {
        this.funcionarioRepository = funcionarioRepository;
        this.pontoRepository = pontoRepository;
    }

    /**
     * Processa um evento de leitura RFID, atualizando o registro de ponto do dia.
     */
    @Transactional
    public void processarEventoRfid(RfidAccessEvent event) {
        if (event == null || event.idUser() == null || event.timestamp() == null) {
            log.warn("Evento RFID ignorado: payload incompleto");
            return;
        }
        if (!"ENTRADA".equals(event.type()) && !"SAIDA".equals(event.type())) {
            log.debug("Evento RFID {} ignorado para idUser={}", event.type(), event.idUser());
            return;
        }

        var funcionario = funcionarioRepository.findByPessoaId(event.idUser()).orElse(null);
        if (funcionario == null) {
            log.warn("Evento RFID de usuário sem funcionário vinculado: idUser={}", event.idUser());
            return;
        }

        Instant timestamp = event.timestamp();
        LocalDate data = timestamp.atZone(ZoneId.systemDefault()).toLocalDate();

        RegistroPontoDiario registro = pontoRepository
                .findByFuncionarioIdAndData(funcionario.getId(), data)
                .orElseGet(() -> {
                    RegistroPontoDiario novo = new RegistroPontoDiario();
                    novo.setFuncionario(funcionario);
                    novo.setData(data);
                    return novo;
                });

        if ("ENTRADA".equals(event.type())) {
            if (registro.getHoraEntrada() == null || timestamp.isBefore(registro.getHoraEntrada())) {
                registro.setHoraEntrada(timestamp);
            }
        } else {
            if (registro.getHoraSaida() == null || timestamp.isAfter(registro.getHoraSaida())) {
                registro.setHoraSaida(timestamp);
            }
        }

        if (registro.getHoraEntrada() != null && registro.getHoraSaida() != null) {
            long minutos = Duration.between(registro.getHoraEntrada(), registro.getHoraSaida()).toMinutes();
            BigDecimal horas = BigDecimal.valueOf(Math.max(0, minutos))
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            registro.setTotalHoras(horas);
        }

        pontoRepository.save(registro);
        log.info("Ponto registrado para {} em {} | entrada={} saída={} total={}",
                funcionario.getId(), data, registro.getHoraEntrada(), registro.getHoraSaida(), registro.getTotalHoras());
    }
}