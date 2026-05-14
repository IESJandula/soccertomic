package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.PlayerProfileRequestDTO;
import com.worldcup.Back.dto.response.PlayerProfileResponseDTO;
import com.worldcup.Back.entity.PlayerProfileEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.exception.ResourceNotFoundException;
import com.worldcup.Back.repository.PlayerProfileRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import com.worldcup.Back.service.level.PartidoRatingEngineService;
import com.worldcup.Back.service.level.VisibleLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class PlayerProfileService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerProfileService.class);
    private static final BigDecimal DEFAULT_MU = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_SIGMA = new BigDecimal("8.33");
    private static final BigDecimal MIN_INITIAL_SIGMA = new BigDecimal("7.50");

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private VisibleLevelService visibleLevelService;

    @Transactional
    public PlayerProfileResponseDTO saveOrUpdateProfile(String firebaseUid, PlayerProfileRequestDTO request) {
        logger.info("🟢 saveOrUpdateProfile - Buscando usuario con UID: {}", firebaseUid);

        UsuarioEntity usuario = userService.obtenerOCrearPorFirebaseUid(firebaseUid, null);

        logger.info("✅ Usuario encontrado: id={}", usuario.getId());

        PlayerProfileEntity entity = playerProfileRepository.findByUsuarioId(usuario.getId())
                .orElseGet(() -> {
                    logger.info("📝 Creando nuevo perfil para usuario: {}", usuario.getId());
                    return new PlayerProfileEntity();
                });
        boolean creatingNewProfile = entity.getId() == null;

        entity.setUsuario(usuario);
        mapAttributes(entity, request.getAttributes());
        logger.info("📦 Perfil mapeado, guardando...");

        PlayerProfileEntity saved = playerProfileRepository.save(entity);
        logger.info("✅ Perfil guardado exitosamente con ID: {}", saved.getId());
        
        usuario.setPlayerProfile(saved);
        aplicarAjusteAutovaloracionInicial(usuario, saved.getSelfAssessment(), creatingNewProfile);

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
        if (usuario == null) {
            return 5;
        }
        return Math.max(1, visibleLevelService.calcularNivelVisible(usuario).setScale(0, RoundingMode.HALF_UP).intValue());
    }

    private void mapAttributes(PlayerProfileEntity entity, PlayerProfileRequestDTO.AttributesDTO attributes) {
        if (attributes == null) {
            return;
        }

        String posicion = firstNonBlank(attributes.getPosicionPreferida(), entity.getPosicionPreferida(), "MEDIOCAMPISTA");
        entity.setPosicionPreferida(posicion);
        entity.setPlayStyle(normalizePlayStyle(attributes.getPlayStyle(), entity.getPlayStyle()));
        entity.setSkillTier(firstNonBlank(attributes.getSkillTier(), entity.getSkillTier(), "BRONCE"));
        entity.setAgeRange(firstNonBlank(attributes.getAgeRange(), entity.getAgeRange(), "18_25"));
        entity.setSelfAssessment(normalizeSelfAssessment(attributes.getSelfAssessment(), entity.getSelfAssessment()));

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
    }

    private PlayerProfileResponseDTO toResponse(PlayerProfileEntity profile) {
        PlayerProfileResponseDTO response = new PlayerProfileResponseDTO();
        response.setUsuarioId(profile.getUsuario().getId());
        response.setActualizadoEn(profile.getActualizadoEn());

        PlayerProfileResponseDTO.AttributesDTO attributes = new PlayerProfileResponseDTO.AttributesDTO();
        attributes.setGoalkeeper(profile.getGoalkeeper());
        attributes.setPosicionPreferida(profile.getPosicionPreferida());
        attributes.setPlayStyle(profile.getPlayStyle());
        attributes.setSkillTier(profile.getSkillTier());
        attributes.setPlayTendency(profile.getPlayTendency());
        attributes.setAgeRange(profile.getAgeRange());
        attributes.setSelfAssessment(profile.getSelfAssessment());
        attributes.setPiernaBuena(profile.getPiernaBuena());
        
        // Convert comma-separated string back to list
        if (profile.getDisponibilidad() != null && !profile.getDisponibilidad().isEmpty()) {
            attributes.setDisponibilidad(java.util.Arrays.asList(profile.getDisponibilidad().split(",")));
        }
        
        response.setAttributes(attributes);

        return response;
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

    private Integer normalizeSelfAssessment(Integer value, Integer fallback) {
        Integer source = value != null ? value : fallback;
        if (source == null) {
            return null;
        }
        if (source < 1) {
            return 1;
        }
        if (source > 5) {
            return 5;
        }
        return source;
    }

    private void aplicarAjusteAutovaloracionInicial(UsuarioEntity usuario, Integer selfAssessment, boolean creatingNewProfile) {
        if (!creatingNewProfile || selfAssessment == null || usuario == null) {
            return;
        }

        int partidosJugados = usuario.getPartidosJugados() == null ? 0 : usuario.getPartidosJugados();
        if (partidosJugados > 0) {
            return;
        }

        BigDecimal offsetMu = switch (selfAssessment) {
            case 1 -> new BigDecimal("-2.00");
            case 2 -> new BigDecimal("-1.00");
            case 4 -> new BigDecimal("1.00");
            case 5 -> new BigDecimal("2.00");
            default -> BigDecimal.ZERO;
        };

        BigDecimal nuevoMu = DEFAULT_MU.add(offsetMu).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sigmaActual = usuario.getRatingSigma() == null ? DEFAULT_SIGMA : usuario.getRatingSigma();
        BigDecimal sigmaInicial = sigmaActual.max(MIN_INITIAL_SIGMA).setScale(2, RoundingMode.HALF_UP);

        usuario.setRatingMu(nuevoMu);
        usuario.setRatingSigma(sigmaInicial);
        usuario.setRatingVersion(PartidoRatingEngineService.RATING_SNAPSHOT_VERSION);
        usuarioRepository.save(usuario);
    }
}
