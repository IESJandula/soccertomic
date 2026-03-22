package com.worldcup.Back.repository;

import com.worldcup.Back.entity.EquipoRapidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipoRapidoRepository extends JpaRepository<EquipoRapidoEntity, Long> {
    List<EquipoRapidoEntity> findByOwnerOrderByActualizadoEnDesc(UsuarioEntity owner);

    Optional<EquipoRapidoEntity> findByIdAndOwner(Long id, UsuarioEntity owner);
}
