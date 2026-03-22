package com.worldcup.Back.repository;

import com.worldcup.Back.entity.InvitacionEntity;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoInvitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitacionRepository extends JpaRepository<InvitacionEntity, Long> {
    List<InvitacionEntity> findByUsuario(UsuarioEntity usuario);
    List<InvitacionEntity> findByPartido(PartidoEntity partido);
    List<InvitacionEntity> findByUsuarioAndEstado(UsuarioEntity usuario, EstadoInvitacion estado);
    Optional<InvitacionEntity> findByPartidoAndUsuario(PartidoEntity partido, UsuarioEntity usuario);
}
