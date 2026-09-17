package com.fablab.notification.controller;

import com.fablab.notification.dto.NotificacaoHistoricoResponse;
import com.fablab.notification.dto.NotificacaoResponse;
import com.fablab.notification.dto.NotificacaoTesteRequest;
import com.fablab.notification.dto.RevisaoRequest;
import com.fablab.notification.entity.CanalNotificacao;
import com.fablab.notification.entity.StatusNotificacao;
import com.fablab.notification.entity.TipoEvento;
import com.fablab.notification.service.NotificacaoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de notificações. */
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    /** Notificações do usuário autenticado. */
    @GetMapping
    public List<NotificacaoResponse> listarMinhas(
            @RequestParam(required = false) StatusNotificacao status,
            @RequestParam(required = false) TipoEvento tipoEvento) {
        return notificacaoService.listarDoUsuario(status, tipoEvento);
    }

    /** Marca uma notificação do próprio usuário como lida. */
    @PutMapping("/{id}/ler")
    public NotificacaoResponse marcarComoLida(@PathVariable Long id) {
        return notificacaoService.marcarComoLida(id);
    }

    /** Lista todas as notificações ativas (Admin). */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificacaoResponse> listarAdmin(
            @RequestParam(required = false) StatusNotificacao status,
            @RequestParam(required = false) TipoEvento tipoEvento) {
        return notificacaoService.listarAdmin(status, tipoEvento);
    }

    /** Move as notificações selecionadas para o histórico (Admin). */
    @PostMapping("/admin/revisar")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> revisar(@Valid @RequestBody RevisaoRequest request) {
        return Map.of("revisadas", notificacaoService.revisar(request.idsNotificacoes()));
    }

    /** Histórico de notificações revisadas (Admin). */
    @GetMapping("/historico")
    @PreAuthorize("hasRole('ADMIN')")
    public List<NotificacaoHistoricoResponse> listarHistorico(
            @RequestParam(required = false) CanalNotificacao canal,
            @RequestParam(required = false) TipoEvento tipoEvento) {
        return notificacaoService.listarHistorico(canal, tipoEvento);
    }

    /** Envia uma notificação de teste (Admin). */
    @PostMapping("/teste")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacaoResponse enviarTeste(@Valid @RequestBody NotificacaoTesteRequest request) {
        return notificacaoService.enviarTeste(request);
    }
}
