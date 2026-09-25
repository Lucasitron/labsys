package com.fablab.notification.repository;

import com.fablab.notification.entity.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acesso aos dados da tabela {@code notificacao}.
 */
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    long countByIdUsuarioAndLidaFalse(Long idUsuario);

    /**
     * Lista do usuário autenticado (D-2: sempre filtrada pelo JWT).
     */
    @Query("""
            SELECT n FROM Notificacao n WHERE n.idUsuario = :idUsuario
            AND (:tipo IS NULL OR n.tipo = :tipo)
            AND (:lida IS NULL OR n.lida = :lida)
            ORDER BY n.criadaEm DESC
            """)
    Page<Notificacao> buscarDoUsuario(@Param("idUsuario") Long idUsuario,
                                      @Param("tipo") String tipo,
                                      @Param("lida") Boolean lida,
                                      Pageable pageable);

    /**
     * Histórico Admin: todas as notificações com filtros opcionais (D-4).
     */
    @Query("""
            SELECT n FROM Notificacao n
            WHERE (:busca IS NULL
                OR LOWER(n.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                OR LOWER(COALESCE(n.mensagem, '')) LIKE LOWER(CONCAT('%', :busca, '%')))
            AND (:tipo IS NULL OR n.tipo = :tipo)
            AND (:lida IS NULL OR n.lida = :lida)
            AND (:canal IS NULL OR n.canal = :canal)
            ORDER BY n.criadaEm DESC
            """)
    java.util.List<Notificacao> buscarHistorico(@Param("busca") String busca,
                                                @Param("tipo") String tipo,
                                                @Param("lida") Boolean lida,
                                                @Param("canal") String canal);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.idUsuario = :idUsuario AND n.lida = false")
    int marcarTodasComoLidas(@Param("idUsuario") Long idUsuario);
}
