package com.fablab.notification.service;

import com.fablab.notification.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas de notificações do usuário autenticado.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long idUsuario) {
        return notificacaoRepository.countByIdUsuarioAndLidaFalse(idUsuario);
    }
}