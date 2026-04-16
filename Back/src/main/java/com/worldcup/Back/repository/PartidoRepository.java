package com.worldcup.Back.repository;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartidoRepository extends JpaRepository<PartidoEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PartidoEntity p WHERE p.id = :id")
    Optional<PartidoEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p
            FROM PartidoEntity p
            JOIN p.organizadores org
            WHERE org.usuario = :usuario
            ORDER BY p.fecha DESC
            """)
    List<PartidoEntity> findByOrganizador(@Param("usuario") UsuarioEntity usuario);

        @Query("""
            SELECT DISTINCT p
            FROM PartidoEntity p
            LEFT JOIN p.jugadoresInscritos ji
            LEFT JOIN p.equipoA ea
            LEFT JOIN p.equipoB eb
            WHERE ji = :usuario OR ea = :usuario OR eb = :usuario
            ORDER BY p.fecha DESC
            """)
        List<PartidoEntity> findByParticipante(@Param("usuario") UsuarioEntity usuario);

    List<PartidoEntity> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
    List<PartidoEntity> findByTipo(String tipo);

        @Query("""
                        SELECT DISTINCT p
                        FROM PartidoEntity p
                        LEFT JOIN p.organizadores org
                        LEFT JOIN p.equipoA ea
                        LEFT JOIN p.equipoB eb
                        WHERE p.estado IN :estados
                            AND (org.usuario = :usuario OR ea = :usuario OR eb = :usuario)
                        ORDER BY p.fecha DESC
                        """)
        List<PartidoEntity> findHistorialFinalizadoDeUsuario(@Param("usuario") UsuarioEntity usuario,
                                                                                                                 @Param("estados") List<EstadoPartido> estados);
}
