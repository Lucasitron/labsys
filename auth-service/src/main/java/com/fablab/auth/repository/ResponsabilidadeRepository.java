package com.fablab.auth.repository;

import com.fablab.auth.entity.Responsabilidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso aos dados da tabela {@code usuario_responsabilidade}.
 */
public interface ResponsabilidadeRepository extends JpaRepository<Responsabilidade, Long> {

    List<Responsabilidade> findByUsuarioId(Long idUsuario);
}