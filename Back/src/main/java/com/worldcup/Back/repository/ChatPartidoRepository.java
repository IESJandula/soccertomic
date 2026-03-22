package com.worldcup.Back.repository;

import com.worldcup.Back.entity.ChatPartidoEntity;
import com.worldcup.Back.entity.PartidoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatPartidoRepository extends JpaRepository<ChatPartidoEntity, Long> {
    List<ChatPartidoEntity> findByPartido(PartidoEntity partido);
}
