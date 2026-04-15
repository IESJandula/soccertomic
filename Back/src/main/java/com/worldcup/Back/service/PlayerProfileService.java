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
        if (attributes == null) {
            return;
        }

        entity.setShooting(normalizeSkill(attributes.getShooting(), entity.getShooting()));
        entity.setSpeed(normalizeSkill(attributes.getSpeed(), entity.getSpeed()));
        entity.setDribbling(normalizeSkill(attributes.getDribbling(), entity.getDribbling()));
        entity.setDefense(normalizeSkill(attributes.getDefense(), entity.getDefense()));
        entity.setStrength(normalizeSkill(attributes.getStrength(), entity.getStrength()));
        entity.setStamina(normalizeSkill(attributes.getStamina(), entity.getStamina()));
        entity.setAerial(normalizeSkill(attributes.getAerial(), entity.getAerial()));

        String posicion = firstNonBlank(attributes.getPosicionPreferida(), entity.getPosicionPreferida(), "MEDIOCAMPISTA");
        entity.setPosicionPreferida(posicion);
        entity.setPlayStyle(normalizePlayStyle(attributes.getPlayStyle(), entity.getPlayStyle()));
        entity.setSkillTier(firstNonBlank(attributes.getSkillTier(), entity.getSkillTier(), "BRONCE"));
        entity.setAgeRange(firstNonBlank(attributes.getAgeRange(), entity.getAgeRange(), "18_25"));

        if (attributes.getGoalkeeper() != null) {
            entity.setGoalkeeper(attributes.getGoalkeeper());
        } else {
            entity.setGoalkeeper("PORTERO".equalsIgnoreCase(posicion));
        }

        if ("G".equalsIgnoreCase(entity.getPlayStyle())) {
            entity.setGoalkeeper(true);
            if (attributes.getPosicionPreferida() == null || attributes.getPosicionPreferida().isBlank()) {
                entity.setPosicionPreferida("PORTERO");
            }
        }

        if (attributes.getPiernaBuena() != null) {
            String pierna = attributes.getPiernaBuena().trim();
            entity.setPiernaBuena(pierna.isEmpty() ? null : pierna);
        }

        if (attributes.getDisponibilidad() != null) {
            if (attributes.getDisponibilidad().isEmpty()) {
                entity.setDisponibilidad(null);
            } else {
                entity.setDisponibilidad(String.join(",", attributes.getDisponibilidad()));
            }
        }

        entity.setPlayTendency(calculatePlayTendency(entity.getPlayStyle()));
        entity.setGlobalRating(calculateGlobalRating(entity));
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

    private Float calculateGlobalRating(PlayerProfileEntity profile) {
        int shooting = normalizeSkill(profile.getShooting(), 3);
        int speed = normalizeSkill(profile.getSpeed(), 3);
        int dribbling = normalizeSkill(profile.getDribbling(), 3);
        int defense = normalizeSkill(profile.getDefense(), 3);
        int strength = normalizeSkill(profile.getStrength(), 3);
        int stamina = normalizeSkill(profile.getStamina(), 3);
        int aerial = normalizeSkill(profile.getAerial(), 3);

        int sum = shooting + speed + dribbling + defense + strength + stamina + aerial;
        float baseRating = (float) sum / 7;
        
        // Añadir modificador según skillTier
        float tierModifier = 0;
        if (profile.getSkillTier() != null) {
            switch (profile.getSkillTier()) {
                case "BRONCE": tierModifier = -1; break;
                case "PLATA": tierModifier = 1; break;
                case "ORO": tierModifier = 3; break;
                case "DIAMANTE": tierModifier = 5; break;
                default: tierModifier = 0; break;
            }
        }
        
        // Calcular rating final y redondear a 1 decimal
        float finalRating = baseRating + tierModifier;
        return Math.round(finalRating * 10) / 10f;
    }

    private String calculatePlayTendency(String playStyle) {
        if ("G".equalsIgnoreCase(playStyle)) {
            return "PORTERO";
        }
        if ("O".equalsIgnoreCase(playStyle)) {
            return "OFENSIVA";
        }
        if ("D".equalsIgnoreCase(playStyle)) {
            return "DEFENSIVA";
        }
        return "ADAPTABLE";
    }

    private int normalizeSkill(Integer value, Integer fallback) {
        int source = value != null ? value : (fallback != null ? fallback : 3);
        if (source < 0) {
            return 0;
        }
        if (source > 5) {
            return 5;
        }
        return source;
    }

    private String normalizePlayStyle(String value, String fallback) {
        String raw = firstNonBlank(value, fallback, "A").toUpperCase();
        return switch (raw) {
            case "O", "D", "A", "G" -> raw;
            default -> "A";
        };
    }

    private String firstNonBlank(String first, String second, String defaultValue) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return defaultValue;
    }
}
