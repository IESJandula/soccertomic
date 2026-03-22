package com.worldcup.Back.repository;

import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.EquipoRapidoInvitacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRapidoInvitacionRepository extends JpaRepository<EquipoRapidoInvitacionEntity, Long> {
    List<EquipoRapidoInvitacionEntity> findByDestinatarioOrderByCreadaEnDesc(UsuarioEntity destinatario);

    List<EquipoRapidoInvitacionEntity> findByDestinatarioAndEstadoOrderByCreadaEnDesc(UsuarioEntity destinatario, EstadoInvitacion estado);

    Optional<EquipoRapidoInvitacionEntity> findByIdAndDestinatario(Long id, UsuarioEntity destinatario);

    boolean existsByEquipoRapidoAndDestinatarioAndEstado(EquipoRapidoEntity equipoRapido, UsuarioEntity destinatario, EstadoInvitacion estado);
}
