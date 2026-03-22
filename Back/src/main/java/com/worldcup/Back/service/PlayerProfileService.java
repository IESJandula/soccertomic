package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.PlayerProfileRequestDTO;
import com.worldcup.Back.dto.response.PlayerProfileResponseDTO;
import com.worldcup.Back.entity.PlayerProfileEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.PlayerProfileRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerProfileService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileService.class);

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public PlayerProfileResponseDTO saveOrUpdateProfile(String firebaseUid, PlayerProfileRequestDTO request) {
        logger.info("🟢 saveOrUpdateProfile - Buscando usuario con UID: {}", firebaseUid);

        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    logger.warn("⚠️ Usuario no encontrado con UID: {}. Creando perfil base de desarrollo.", firebaseUid);
                    UsuarioEntity nuevo = new UsuarioEntity();
                    nuevo.setFirebaseUid(firebaseUid);
                    nuevo.setNombre(firebaseUid);
                    nuevo.setEmail(firebaseUid + "@local.dev");
                    return usuarioRepository.save(nuevo);
                });

        logger.info("✅ Usuario encontrado: id={}", usuario.getId());

        PlayerProfileEntity entity = playerProfileRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    logger.info("📝 Creando nuevo perfil para usuario: {}", usuario.getId());
                    return new PlayerProfileEntity();
                });

        entity.setUsuario(usuario);
        mapAttributes(entity, request.getAttributes());
        logger.info("📦 Perfil mapeado, guardando...");

        PlayerProfileEntity saved = playerProfileRepository.save(entity);
        logger.info("✅ Perfil guardado exitosamente con ID: {}", saved.getId());
        
        usuario.setPlayerProfile(saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PlayerProfileResponseDTO getProfile(String firebaseUid) {
        UsuarioEntity usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        PlayerProfileEntity profile = playerProfileRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de jugador no encontrado"));

        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public Optional<PlayerProfileEntity> findByUsuarioId(Long usuarioId) {
        return playerProfileRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public PlayerProfileResponseDTO getProfileByUsuarioId(Long usuarioId) {
        PlayerProfileEntity profile = playerProfileRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de jugador no encontrado"));

        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public int getPlayerLevel(UsuarioEntity usuario) {
        Optional<PlayerProfileEntity> profile = findByUsuarioId(usuario.getId());
        if (profile.isPresent() && profile.get().getGlobalRating() != null) {
            // Convertir globalRating (0-5) a escala de nivel (1-10)
            return Math.round(profile.get().getGlobalRating() * 2);
        }
        // Fallback: retornar 5 como nivel por defecto
        return 5;
    }

    private void mapAttributes(PlayerProfileEntity entity, PlayerProfileRequestDTO.AttributesDTO attributes) {
        entity.setShooting(attributes.getShooting());
        entity.setSpeed(attributes.getSpeed());
        entity.setDribbling(attributes.getDribbling());
        entity.setDefense(attributes.getDefense());
        entity.setStrength(attributes.getStrength());
        entity.setStamina(attributes.getStamina());
        entity.setAerial(attributes.getAerial());
        entity.setGoalkeeper(attributes.getGoalkeeper());
        entity.setPosicionPreferida(attributes.getPosicionPreferida());
        entity.setPlayStyle(attributes.getPlayStyle());
        entity.setSkillTier(attributes.getSkillTier());
        entity.setAgeRange(attributes.getAgeRange());
        entity.setPiernaBuena(attributes.getPiernaBuena());
        
        // Store disponibilidad as JSON string
        if (attributes.getDisponibilidad() != null && !attributes.getDisponibilidad().isEmpty()) {
            entity.setDisponibilidad(String.join(",", attributes.getDisponibilidad()));
        }
        
        // Calcular tendencia de juego basada en atributos
        entity.setPlayTendency(calculatePlayTendency(attributes));
        
        // Calcular rating global (incluye modificador de skillTier)
        entity.setGlobalRating(calculateGlobalRating(attributes));
    }

    private PlayerProfileResponseDTO toResponse(PlayerProfileEntity profile) {
        PlayerProfileResponseDTO response = new PlayerProfileResponseDTO();
        response.setUsuarioId(profile.getUsuario().getId());
        response.setActualizadoEn(profile.getActualizadoEn());

        PlayerProfileResponseDTO.AttributesDTO attributes = new PlayerProfileResponseDTO.AttributesDTO();
        attributes.setShooting(profile.getShooting());
        attributes.setSpeed(profile.getSpeed());
        attributes.setDribbling(profile.getDribbling());
        attributes.setDefense(profile.getDefense());
        attributes.setStrength(profile.getStrength());
        attributes.setStamina(profile.getStamina());
        attributes.setAerial(profile.getAerial());
        attributes.setGoalkeeper(profile.getGoalkeeper());
        attributes.setPosicionPreferida(profile.getPosicionPreferida());
        attributes.setPlayStyle(profile.getPlayStyle());
        attributes.setSkillTier(profile.getSkillTier());
        attributes.setPlayTendency(profile.getPlayTendency());
        attributes.setAgeRange(profile.getAgeRange());
        attributes.setGlobalRating(profile.getGlobalRating());
        attributes.setPiernaBuena(profile.getPiernaBuena());
        
        // Convert comma-separated string back to list
        if (profile.getDisponibilidad() != null && !profile.getDisponibilidad().isEmpty()) {
            attributes.setDisponibilidad(java.util.Arrays.asList(profile.getDisponibilidad().split(",")));
        }
        
        response.setAttributes(attributes);

        return response;
    }

    private Float calculateGlobalRating(PlayerProfileRequestDTO.AttributesDTO attributes) {
        List<Integer> skills = new ArrayList<>();
        skills.add(attributes.getShooting());
        skills.add(attributes.getSpeed());
        skills.add(attributes.getDribbling());
        skills.add(attributes.getDefense());
        skills.add(attributes.getStrength());
        skills.add(attributes.getStamina());
        skills.add(attributes.getAerial());

        // Calcular promedio base (0-5)
        int sum = skills.stream().mapToInt(Integer::intValue).sum();
        float baseRating = (float) sum / 7;
        
        // Añadir modificador según skillTier
        float tierModifier = 0;
        if (attributes.getSkillTier() != null) {
            switch (attributes.getSkillTier()) {
                case "BRONCE": tierModifier = -1; break;
                case "PLATA": tierModifier = 1; break;
                case "ORO": tierModifier = 3; break;
                case "DIAMANTE": tierModifier = 5; break;
            }
        }
        
        // Calcular rating final y redondear a 1 decimal
        float finalRating = baseRating + tierModifier;
        return Math.round(finalRating * 10) / 10f;
    }
    
    private String calculatePlayTendency(PlayerProfileRequestDTO.AttributesDTO attributes) {
        // Calcular puntuación ofensiva (shooting, speed, dribbling)
        int offensive = attributes.getShooting() + attributes.getSpeed() + attributes.getDribbling();
        
        // Calcular puntuación defensiva (defense, strength, aerial)
        int defensive = attributes.getDefense() + attributes.getStrength() + attributes.getAerial();
        
        // Diferencia para determinar tendencia
        int diff = Math.abs(offensive - defensive);
        
        // Si la diferencia es menor a 3, es adaptable
        if (diff < 3) {
            return "ADAPTABLE";
        }
        
        // Si es mayor, determinar si es ofensiva o defensiva
        return offensive > defensive ? "OFENSIVA" : "DEFENSIVA";
    }
}
