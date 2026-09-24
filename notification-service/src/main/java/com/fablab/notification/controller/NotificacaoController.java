package com.fablab.notification.controller;

import com.fablab.notification.dto.AuthPrincipal;
import com.fablab.notification.dto.UnreadCountResponse;
import com.fablab.notification.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de notificações do App Shell.
 */
@RestController
@RequestMapping("/notifications")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/unread/count")
    public ResponseEntity<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(new UnreadCountResponse(notificacaoService.contarNaoLidas(principal.idUser())));
    }
}