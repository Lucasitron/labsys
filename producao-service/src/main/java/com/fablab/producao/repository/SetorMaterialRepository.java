package com.fablab.producao.repository;

import com.fablab.producao.entity.SetorMaterial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorMaterialRepository extends JpaRepository<SetorMaterial, Long> {

    List<SetorMaterial> findBySetor_IdSetor(Long idSetor);
}