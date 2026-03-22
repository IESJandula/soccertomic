package com.worldcup.Back.repository;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartidoOrganizadorRepository extends JpaRepository<PartidoOrganizadorEntity, Long> {
    List<PartidoOrganizadorEntity> findByPartidoOrderByAsignadoEnAsc(PartidoEntity partido);
    List<PartidoOrganizadorEntity> findByPartidoAndRolOrderByAsignadoEnAsc(PartidoEntity partido, PartidoOrganizadorRol rol);
    Optional<PartidoOrganizadorEntity> findByPartidoAndUsuario(PartidoEntity partido, UsuarioEntity usuario);
    boolean existsByPartidoAndUsuario(PartidoEntity partido, UsuarioEntity usuario);
    List<PartidoOrganizadorEntity> findByUsuario(UsuarioEntity usuario);
    void deleteByPartido(PartidoEntity partido);
}
