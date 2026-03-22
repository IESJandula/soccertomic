package com.worldcup.Back.controller;

import com.worldcup.Back.dto.request.PlayerProfileRequestDTO;
import com.worldcup.Back.dto.response.PlayerProfileResponseDTO;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.security.FirebaseRequestContext;
import com.worldcup.Back.service.AmistadService;
import com.worldcup.Back.service.PlayerProfileService;
import com.worldcup.Back.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/player-profile")
public class PlayerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileController.class);

    @Autowired
    private PlayerProfileService playerProfileService;

    @Autowired
    private UserService userService;

    @Autowired
    private AmistadService amistadService;

    @PutMapping("/me")
    public ResponseEntity<PlayerProfileResponseDTO> saveOrUpdate(
            HttpServletRequest request,
            @Valid @RequestBody PlayerProfileRequestDTO dto
    ) {
        String uid = FirebaseRequestContext.requireUid(request);
        logger.info("🟢 PlayerProfileController.saveOrUpdate - UID: {}", uid);
        try {
            logger.info("📦 Payload recibido: {}", dto);
            PlayerProfileResponseDTO response = playerProfileService.saveOrUpdateProfile(uid, dto);
            logger.info("✅ Perfil guardado exitosamente para UID: {}", uid);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Error de validación: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (ResourceNotFoundException e) {
            logger.error("❌ Usuario no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<PlayerProfileResponseDTO> getMyProfile(HttpServletRequest request) {
        String uid = FirebaseRequestContext.requireUid(request);
        logger.info("🟢 PlayerProfileController.getMyProfile - UID: {}", uid);
        try {
            PlayerProfileResponseDTO response = playerProfileService.getProfile(uid);
            logger.info("✅ Perfil recuperado para UID: {}", uid);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            logger.info("ℹ️ Perfil no encontrado para UID: {} (esperado)", uid);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error inesperado: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/public/{usuarioId}")
    public ResponseEntity<PlayerProfileResponseDTO> getPublicFriendProfile(
            HttpServletRequest request,
            @PathVariable Long usuarioId
    ) {
        String uid = FirebaseRequestContext.requireUid(request);

        Optional<UsuarioEntity> requester = userService.buscarPorFirebaseUid(uid);
        Optional<UsuarioEntity> target = userService.buscarPorId(usuarioId);

        if (requester.isEmpty() || target.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (!amistadService.sonAmigos(requester.get(), target.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            PlayerProfileResponseDTO response = playerProfileService.getProfileByUsuarioId(usuarioId);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("❌ Error recuperando perfil público de usuario {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}