package com.worldcup.Back.repository;

import com.worldcup.Back.entity.AmistadEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoAmistad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmistadRepository extends JpaRepository<AmistadEntity, Long> {
    List<AmistadEntity> findByUsuarioA(UsuarioEntity usuario);
    List<AmistadEntity> findByUsuarioB(UsuarioEntity usuario);
    List<AmistadEntity> findByUsuarioAAndEstado(UsuarioEntity usuario, EstadoAmistad estado);
    List<AmistadEntity> findByUsuarioBAndEstado(UsuarioEntity usuario, EstadoAmistad estado);
    Optional<AmistadEntity> findByUsuarioAAndUsuarioB(UsuarioEntity usuarioA, UsuarioEntity usuarioB);

    @Query("""
        SELECT a
        FROM AmistadEntity a
        WHERE a.estado = :estado
          AND (a.usuarioA = :usuario OR a.usuarioB = :usuario)
        """)
    List<AmistadEntity> findByUsuarioAndEstado(@Param("usuario") UsuarioEntity usuario,
                           @Param("estado") EstadoAmistad estado);

    @Query("""
        SELECT a
        FROM AmistadEntity a
        WHERE (a.usuarioA = :usuarioA AND a.usuarioB = :usuarioB)
           OR (a.usuarioA = :usuarioB AND a.usuarioB = :usuarioA)
        """)
    Optional<AmistadEntity> findBetweenUsuarios(@Param("usuarioA") UsuarioEntity usuarioA,
                        @Param("usuarioB") UsuarioEntity usuarioB);

    @Query("""
        SELECT a
        FROM AmistadEntity a
        WHERE a.estado = :estado
          AND a.usuarioA = :usuario
        """)
    List<AmistadEntity> findSolicitudesEnviadas(@Param("usuario") UsuarioEntity usuario,
                        @Param("estado") EstadoAmistad estado);
}
