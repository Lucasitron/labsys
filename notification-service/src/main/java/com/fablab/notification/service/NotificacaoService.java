package com.fablab.notification.service;

import com.fablab.notification.dto.HistoryPayload;
import com.fablab.notification.dto.NotificationResponse;
import com.fablab.notification.dto.PagedNotifications;
import com.fablab.notification.entity.Notificacao;
import com.fablab.notification.exception.ForbiddenException;
import com.fablab.notification.exception.ResourceNotFoundException;
import com.fablab.notification.repository.NotificacaoRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultas e comandos de notificações (§6.3 D-1..D-7).
 */
@Service
public class NotificacaoService {

    static final Set<String> TIPOS = Set.of(
            "encomenda", "estoque", "financeiro", "producao", "pessoas", "sistema");
    static final Set<String> CANAIS = Set.of("inapp", "email", "push");

    private final NotificacaoRepository notificacaoRepository;
    private final AtomicReference<Map<String, Map<String, Boolean>>> preferencias;

    public NotificacaoService(NotificacaoRepository notificacaoRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.preferencias = new AtomicReference<>(preferenciasPadrao());
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long idUsuario) {
        return notificacaoRepository.countByIdUsuarioAndLidaFalse(idUsuario);
    }

    /**
     * Lista paginada canônica do usuário (D-1/D-2). Página 1-based.
     */
    @Transactional(readOnly = true)
    public PagedNotifications listar(Long idUsuario, int page, int size, String tipo, Boolean lida) {
        if (page < 1) {
            throw new IllegalArgumentException("Página inválida: use valores a partir de 1");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Tamanho de página inválido: use valores entre 1 e 100");
        }
        Page<Notificacao> pagina = notificacaoRepository.buscarDoUsuario(
                idUsuario, normalizar(tipo), lida, PageRequest.of(page - 1, size));
        List<NotificationResponse> items = pagina.getContent().stream().map(this::paraResponse).toList();
        return new PagedNotifications(items, pagina.getTotalElements(), page, size, size,
                pagina.getTotalPages());
    }

    @Transactional
    public NotificationResponse marcarComoLida(Long id, Long idUsuario, boolean admin) {
        Notificacao notificacao = notificacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        if (!admin && !notificacao.getIdUsuario().equals(idUsuario)) {
            throw new ForbiddenException("Você só pode ler as suas próprias notificações");
        }
        notificacao.setLida(true);
        return paraResponse(notificacaoRepository.save(notificacao));
    }

    @Transactional
    public long marcarTodasComoLidas(Long idUsuario) {
        return notificacaoRepository.marcarTodasComoLidas(idUsuario);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Boolean>> obterPreferencias() {
        return preferenciasPadraoCopia(preferencias.get());
    }

    @Transactional
    public Map<String, Map<String, Boolean>> salvarPreferencias(Map<String, Map<String, Boolean>> novas) {
        if (novas == null || novas.isEmpty()) {
            throw new IllegalArgumentException("Preferências inválidas: informe a matriz tipo × canal");
        }
        for (Map.Entry<String, Map<String, Boolean>> tipo : novas.entrySet()) {
            if (!TIPOS.contains(tipo.getKey())) {
                throw new IllegalArgumentException("Tipo de notificação inválido: " + tipo.getKey());
            }
            if (tipo.getValue() == null || tipo.getValue().isEmpty()) {
                throw new IllegalArgumentException(
                        "Preferências inválidas: informe os canais do tipo " + tipo.getKey());
            }
            for (Map.Entry<String, Boolean> canal : tipo.getValue().entrySet()) {
                if (!CANAIS.contains(canal.getKey())) {
                    throw new IllegalArgumentException("Canal inválido: " + canal.getKey());
                }
                if (canal.getValue() == null) {
                    throw new IllegalArgumentException(
                            "Preferências inválidas: valor ausente em " + tipo.getKey() + "/" + canal.getKey());
                }
            }
        }
        Map<String, Map<String, Boolean>> copia = new LinkedHashMap<>();
        novas.forEach((tipo, canais) -> copia.put(tipo, new LinkedHashMap<>(canais)));
        preferencias.set(copia);
        return preferenciasPadraoCopia(copia);
    }

    /**
     * Histórico Admin com filtros opcionais; retorna a lista completa (D-4/D-7).
     */
    @Transactional(readOnly = true)
    public HistoryPayload historico(String busca, String tipo, Boolean lida, String canal, String periodo) {
        Instant corte = resolverPeriodo(periodo);
        List<NotificationResponse> items = notificacaoRepository
                .buscarHistorico(normalizar(busca), normalizar(tipo), lida, normalizar(canal)).stream()
                .filter(n -> corte == null || !n.getCriadaEm().isBefore(corte))
                .map(this::paraResponse)
                .toList();
        return new HistoryPayload(items, items.size());
    }

    /**
     * Registro interno (eventos futuros): valida o link (D-6, 422 se inválido).
     */
    @Transactional
    public Notificacao registrar(Long idUsuario, String tipo, String canal, String titulo,
                                 String mensagem, String link) {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("Título da notificação é obrigatório");
        }
        LinkValidator.assertValido(link);
        Notificacao notificacao = new Notificacao();
        notificacao.setIdUsuario(idUsuario);
        notificacao.setTipo(tipo);
        notificacao.setCanal(canal);
        notificacao.setTitulo(titulo.strip());
        notificacao.setMensagem(mensagem);
        notificacao.setLink(link == null ? null : link.strip());
        notificacao.setLida(false);
        notificacao.setCriadaEm(Instant.now());
        return notificacaoRepository.save(notificacao);
    }

    private Instant resolverPeriodo(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            return null;
        }
        String valor = periodo.strip().toLowerCase();
        try {
            if (valor.endsWith("d")) {
                return Instant.now().minusSeconds(Long.parseLong(valor.substring(0, valor.length() - 1)) * 86400);
            }
            if (valor.endsWith("h")) {
                return Instant.now().minusSeconds(Long.parseLong(valor.substring(0, valor.length() - 1)) * 3600);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Período inválido: use formatos como 24h ou 30d");
        }
        throw new IllegalArgumentException("Período inválido: use formatos como 24h ou 30d");
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.strip();
    }

    private NotificationResponse paraResponse(Notificacao notificacao) {
        return new NotificationResponse(
                notificacao.getId(),
                notificacao.getTipo(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                Boolean.TRUE.equals(notificacao.getLida()),
                notificacao.getCriadaEm(),
                notificacao.getLink(),
                notificacao.getCanal());
    }

    private Map<String, Map<String, Boolean>> preferenciasPadrao() {
        Map<String, Map<String, Boolean>> padrao = new LinkedHashMap<>();
        for (String tipo : TIPOS) {
            Map<String, Boolean> canais = new LinkedHashMap<>();
            for (String canal : CANAIS) {
                canais.put(canal, !(tipo.equals("sistema") && canal.equals("push")));
            }
            padrao.put(tipo, canais);
        }
        return padrao;
    }

    private Map<String, Map<String, Boolean>> preferenciasPadraoCopia(Map<String, Map<String, Boolean>> origem) {
        Map<String, Map<String, Boolean>> copia = new LinkedHashMap<>();
        origem.forEach((tipo, canais) -> copia.put(tipo, new LinkedHashMap<>(canais)));
        return copia;
    }
}
