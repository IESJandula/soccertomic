package com.worldcup.Back.repository;

import com.worldcup.Back.entity.EquipoRapidoNotificacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EquipoRapidoNotificacionRepository extends JpaRepository<EquipoRapidoNotificacionEntity, Long> {
    @Query("SELECT n FROM EquipoRapidoNotificacionEntity n LEFT JOIN FETCH n.actor LEFT JOIN FETCH n.destinatario WHERE n.destinatario = :destinatario ORDER BY n.creadaEn DESC")
    List<EquipoRapidoNotificacionEntity> findByDestinatarioOrderByCreadaEnDesc(@Param("destinatario") UsuarioEntity destinatario);

    java.util.Optional<EquipoRapidoNotificacionEntity> findByIdAndDestinatario(Long id, UsuarioEntity destinatario);
}