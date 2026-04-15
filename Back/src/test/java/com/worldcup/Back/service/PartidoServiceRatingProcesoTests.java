package com.worldcup.Back.service;

import com.worldcup.Back.dto.response.PartidoRatingProcesoResponseDTO;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.repository.PartidoIncidenciaRepository;
import com.worldcup.Back.repository.PartidoOrganizadorRepository;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import com.worldcup.Back.service.level.PartidoRatingEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceRatingProcesoTests {

    @Mock
    private PartidoRepository partidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PartidoOrganizadorRepository partidoOrganizadorRepository;

    @Mock
    private PartidoVotacionRepository partidoVotacionRepository;

    @Mock
    private PartidoIncidenciaRepository partidoIncidenciaRepository;

    @Mock
    private PlayerProfileService playerProfileService;

    @Mock
    private TeamBalancingService teamBalancingService;

    @Mock
    private InvitacionService invitacionService;

    @Mock
    private PartidoRatingEngineService partidoRatingEngineService;

    @InjectMocks
    private PartidoService partidoService;

    @Test
    void procesarRatingPartido_aplicaYMarcaIdempotencia() {
        UsuarioEntity owner = usuario(1L, "Owner");
        UsuarioEntity a = usuario(2L, "A");
        UsuarioEntity b = usuario(3L, "B");

        PartidoEntity partido = new PartidoEntity();
        partido.setId(88L);
        partido.setEstado(EstadoPartido.FINALIZADO);
        partido.setEquipoA(new ArrayList<>(List.of(a)));
        partido.setEquipoB(new ArrayList<>(List.of(b)));
        partido.setRatingProcesado(false);
        partido.setEstadoCalidad("NORMAL");

        PartidoOrganizadorEntity relacionOwner = new PartidoOrganizadorEntity();
        relacionOwner.setPartido(partido);
        relacionOwner.setUsuario(owner);
        relacionOwner.setRol(PartidoOrganizadorRol.OWNER);

        when(partidoRepository.findById(88L)).thenReturn(Optional.of(partido));
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner));
        when(usuarioRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoRatingEngineService.procesar(partido)).thenReturn(
            new PartidoRatingEngineService.EngineResult("GANA_A", 2, 2, 0, List.of(a, b))
        );

        PartidoRatingProcesoResponseDTO response = partidoService.procesarRatingPartido(88L, owner);

        assertEquals(true, response.getProcesado());
        assertEquals(false, response.getYaProcesado());
        assertEquals("trueskill-adapted-v1", response.getVersion());
        assertEquals(2, response.getJugadoresActualizados());
        assertEquals(2, response.getVotosConsiderados());
        assertEquals(0, response.getVotosAtipicos());
        assertEquals(true, partido.getRatingProcesado());
        assertEquals("trueskill-adapted-v1", partido.getRatingSnapshotVersion());
    }

    @Test
    void procesarRatingPartido_siYaProcesadoNoDuplica() {
        UsuarioEntity owner = usuario(1L, "Owner");

        PartidoEntity partido = new PartidoEntity();
        partido.setId(89L);
        partido.setEstado(EstadoPartido.FINALIZADO);
        partido.setRatingProcesado(true);
        partido.setRatingSnapshotVersion("trueskill-adapted-v1");
        partido.setRatingProcesadoEn(LocalDateTime.now());

        PartidoOrganizadorEntity relacionOwner = new PartidoOrganizadorEntity();
        relacionOwner.setPartido(partido);
        relacionOwner.setUsuario(owner);
        relacionOwner.setRol(PartidoOrganizadorRol.OWNER);

        when(partidoRepository.findById(89L)).thenReturn(Optional.of(partido));
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner));

        PartidoRatingProcesoResponseDTO response = partidoService.procesarRatingPartido(89L, owner);

        assertEquals(true, response.getYaProcesado());
        assertEquals(false, response.getProcesado());
        assertEquals("YA_PROCESADO", response.getResultadoResolucion());
        assertEquals(0, response.getJugadoresActualizados());
    }

    private UsuarioEntity usuario(Long id, String nombre) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        return usuario;
    }
}
