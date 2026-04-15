package com.worldcup.Back.repository;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoIncidenciaEntity;
import com.worldcup.Back.entity.enums.TipoIncidenciaPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartidoIncidenciaRepository extends JpaRepository<PartidoIncidenciaEntity, Long> {
    List<PartidoIncidenciaEntity> findByPartidoOrderByCreadaEnDesc(PartidoEntity partido);
    long countByPartidoAndTipoIncidencia(PartidoEntity partido, TipoIncidenciaPartido tipoIncidencia);
}
