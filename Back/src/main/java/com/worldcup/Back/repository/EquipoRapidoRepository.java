package com.worldcup.Back.repository;

import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRapidoRepository extends JpaRepository<EquipoRapidoEntity, Long> {
    @Query("SELECT DISTINCT e FROM EquipoRapidoEntity e LEFT JOIN FETCH e.miembros WHERE e.owner = :owner ORDER BY e.actualizadoEn DESC")
    List<EquipoRapidoEntity> findByOwnerOrderByActualizadoEnDesc(@Param("owner") UsuarioEntity owner);

    @Query("SELECT e FROM EquipoRapidoEntity e LEFT JOIN FETCH e.miembros WHERE e.id = :id AND e.owner = :owner")
    Optional<EquipoRapidoEntity> findByIdAndOwner(@Param("id") Long id, @Param("owner") UsuarioEntity owner);

    @Query("SELECT e FROM EquipoRapidoEntity e LEFT JOIN FETCH e.miembros WHERE e.id = :id")
    Optional<EquipoRapidoEntity> findByIdWithMiembros(@Param("id") Long id);
}
