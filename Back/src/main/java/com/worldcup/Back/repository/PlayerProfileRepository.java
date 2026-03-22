package com.worldcup.Back.repository;

import com.worldcup.Back.entity.PlayerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfileEntity, Long> {
    Optional<PlayerProfileEntity> findByUsuarioId(Long usuarioId);
}