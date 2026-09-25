package com.fablab.notification.controller;

import com.fablab.notification.dto.AuthPrincipal;
import com.fablab.notification.dto.HistoryPayload;
import com.fablab.notification.dto.NotificationResponse;
import com.fablab.notification.dto.PagedNotifications;
import com.fablab.notification.dto.UnreadCountResponse;
import com.fablab.notification.service.NotificacaoService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de notificações (§6.3 D-1..D-7).
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

    /**
     * Lista paginada do usuário autenticado (D-1/D-2: isolada pelo JWT).
     */
    @GetMapping
    public ResponseEntity<PagedNotifications> listar(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @AuthenticationPrincipal AuthPrincipal principal) {
        int tamanho = size != null ? size : (pageSize != null ? pageSize : 10);
        return ResponseEntity.ok(
                notificacaoService.listar(principal.idUser(), page, tamanho, type, read));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> marcarComoLida(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPrincipal principal) {
        boolean admin = "ADMIN".equals(principal.role());
        return ResponseEntity.ok(notificacaoService.marcarComoLida(id, principal.idUser(), admin));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Long>> marcarTodasComoLidas(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(
                Map.of("lidas", notificacaoService.marcarTodasComoLidas(principal.idUser())));
    }

    @GetMapping("/preferences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, Boolean>>> obterPreferencias() {
        return ResponseEntity.ok(notificacaoService.obterPreferencias());
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Map<String, Boolean>>> salvarPreferencias(
            @RequestBody Map<String, Map<String, Boolean>> preferencias) {
        return ResponseEntity.ok(notificacaoService.salvarPreferencias(preferencias));
    }

    /**
     * Histórico Admin: lista completa com filtros opcionais (D-3/D-4/D-7).
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HistoryPayload> historico(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(notificacaoService.historico(search, type, read, channel, period));
    }
}
