package com.worldcup.Back.repository;

import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.EquipoRapidoInvitacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRapidoInvitacionRepository extends JpaRepository<EquipoRapidoInvitacionEntity, Long> {
    @Query("SELECT DISTINCT i FROM EquipoRapidoInvitacionEntity i LEFT JOIN FETCH i.equipoRapido WHERE i.destinatario = :destinatario ORDER BY i.creadaEn DESC")
    List<EquipoRapidoInvitacionEntity> findByDestinatarioOrderByCreadaEnDesc(@Param("destinatario") UsuarioEntity destinatario);

    @Query("SELECT DISTINCT i FROM EquipoRapidoInvitacionEntity i LEFT JOIN FETCH i.equipoRapido WHERE i.destinatario = :destinatario AND i.estado = :estado ORDER BY i.creadaEn DESC")
    List<EquipoRapidoInvitacionEntity> findByDestinatarioAndEstadoOrderByCreadaEnDesc(@Param("destinatario") UsuarioEntity destinatario, @Param("estado") EstadoInvitacion estado);

    @Query("SELECT i FROM EquipoRapidoInvitacionEntity i LEFT JOIN FETCH i.equipoRapido WHERE i.id = :id AND i.destinatario = :destinatario")
    Optional<EquipoRapidoInvitacionEntity> findByIdAndDestinatario(@Param("id") Long id, @Param("destinatario") UsuarioEntity destinatario);

    boolean existsByEquipoRapidoAndDestinatarioAndEstado(EquipoRapidoEntity equipoRapido, UsuarioEntity destinatario, EstadoInvitacion estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EquipoRapidoInvitacionEntity i WHERE i.equipoRapido.id = :equipoRapidoId")
    int deleteByEquipoRapidoId(@Param("equipoRapidoId") Long equipoRapidoId);
}
