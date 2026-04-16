package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.PlayerProfileRequestDTO;
import com.worldcup.Back.dto.response.PlayerProfileResponseDTO;
import com.worldcup.Back.entity.PlayerProfileEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.repository.PlayerProfileRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceSelfAssessmentTests {

    @Mock
    private PlayerProfileRepository playerProfileRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PlayerProfileService playerProfileService;

    @Test
    void saveOrUpdateProfile_newProfile_aplicaOffsetMuPorAutovaloracion() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(1L);
        usuario.setFirebaseUid("uid-1");
        usuario.setPartidosJugados(0);
        usuario.setRatingMu(new BigDecimal("25.00"));
        usuario.setRatingSigma(new BigDecimal("8.33"));

        when(usuarioRepository.findByFirebaseUid("uid-1")).thenReturn(Optional.of(usuario));
        when(playerProfileRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());
        when(playerProfileRepository.save(any(PlayerProfileEntity.class))).thenAnswer(invocation -> {
            PlayerProfileEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerProfileRequestDTO request = buildRequest(5);

        PlayerProfileResponseDTO response = playerProfileService.saveOrUpdateProfile("uid-1", request);

        assertNotNull(response);
        assertEquals(5, response.getAttributes().getSelfAssessment());
        assertEquals(new BigDecimal("27.00"), usuario.getRatingMu());
        assertEquals(new BigDecimal("8.33"), usuario.getRatingSigma());
        assertEquals("trueskill-adapted-v2-selfinit", usuario.getRatingVersion());
    }

    @Test
    void saveOrUpdateProfile_existingProfile_noReaplicaOffsetMu() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(2L);
        usuario.setFirebaseUid("uid-2");
        usuario.setPartidosJugados(0);
        usuario.setRatingMu(new BigDecimal("25.00"));
        usuario.setRatingSigma(new BigDecimal("8.33"));

        PlayerProfileEntity existing = new PlayerProfileEntity();
        existing.setId(22L);
        existing.setUsuario(usuario);

        when(usuarioRepository.findByFirebaseUid("uid-2")).thenReturn(Optional.of(usuario));
        when(playerProfileRepository.findByUsuarioId(2L)).thenReturn(Optional.of(existing));
        when(playerProfileRepository.save(any(PlayerProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerProfileRequestDTO request = buildRequest(5);

        PlayerProfileResponseDTO response = playerProfileService.saveOrUpdateProfile("uid-2", request);

        assertNotNull(response);
        assertEquals(5, response.getAttributes().getSelfAssessment());
        assertEquals(new BigDecimal("25.00"), usuario.getRatingMu());
        assertEquals(new BigDecimal("8.33"), usuario.getRatingSigma());
    }

    private PlayerProfileRequestDTO buildRequest(Integer selfAssessment) {
        PlayerProfileRequestDTO.AttributesDTO attributes = new PlayerProfileRequestDTO.AttributesDTO();
        attributes.setPlayStyle("A");
        attributes.setPosicionPreferida("MEDIOCAMPISTA");
        attributes.setSelfAssessment(selfAssessment);

        PlayerProfileRequestDTO request = new PlayerProfileRequestDTO();
        request.setAttributes(attributes);
        return request;
    }
}
