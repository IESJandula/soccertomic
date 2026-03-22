package com.worldcup.Back.repository;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoVotacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartidoVotacionRepository extends JpaRepository<PartidoVotacionEntity, Long> {
    Optional<PartidoVotacionEntity> findByPartidoAndVotante(PartidoEntity partido, UsuarioEntity votante);
    List<PartidoVotacionEntity> findByPartido(PartidoEntity partido);
}
