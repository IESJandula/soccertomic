package com.worldcup.Back.repository;

import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
	Optional<UsuarioEntity> findByEmail(String email);
	Optional<UsuarioEntity> findByFirebaseUid(String firebaseUid);
	boolean existsByEmail(String email);
}
