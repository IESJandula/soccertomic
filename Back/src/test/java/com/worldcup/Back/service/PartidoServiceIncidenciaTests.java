package com.worldcup.Back.service;

import com.worldcup.Back.dto.request.PartidoIncidenciaRequestDTO;
import com.worldcup.Back.dto.response.PartidoIncidenciaResumenDTO;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoIncidenciaEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.entity.enums.TipoIncidenciaPartido;
import com.worldcup.Back.repository.PartidoIncidenciaRepository;
import com.worldcup.Back.repository.PartidoOrganizadorRepository;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceIncidenciaTests {

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

    @InjectMocks
    private PartidoService partidoService;

    @Test
    void registrarIncidencia_actualizaCalidadYResumenOperativo() {
        UsuarioEntity owner = usuario(1L, "Owner");
        UsuarioEntity jugador = usuario(2L, "Jugador Afectado");

        PartidoEntity partido = new PartidoEntity();
        partido.setId(77L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(jugador)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setEstadoCalidad("NORMAL");
        partido.setScoreCalidad(new BigDecimal("1.000"));
        partido.setParticipacionVotacion(new BigDecimal("0.000"));

        PartidoOrganizadorEntity relacionOwner = new PartidoOrganizadorEntity();
        relacionOwner.setPartido(partido);
        relacionOwner.setUsuario(owner);
        relacionOwner.setRol(PartidoOrganizadorRol.OWNER);

        when(partidoRepository.findById(77L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(jugador));
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner));
        when(partidoVotacionRepository.findByPartido(partido)).thenReturn(List.of());

        PartidoIncidenciaEntity incidenciaGuardada = new PartidoIncidenciaEntity();
        incidenciaGuardada.setId(500L);
        incidenciaGuardada.setPartido(partido);
        incidenciaGuardada.setUsuarioAfectado(jugador);
        incidenciaGuardada.setReportadoPor(owner);
        incidenciaGuardada.setTipoIncidencia(TipoIncidenciaPartido.LESION);
        incidenciaGuardada.setSeveridad(3);
        incidenciaGuardada.setMinuto(35);
        incidenciaGuardada.setComentario("Lesion muscular");
        incidenciaGuardada.setValidadaPorOrganizador(true);
        incidenciaGuardada.setCreadaEn(LocalDateTime.now());

        when(partidoIncidenciaRepository.save(any(PartidoIncidenciaEntity.class))).thenReturn(incidenciaGuardada);
        when(partidoIncidenciaRepository.findByPartidoOrderByCreadaEnDesc(partido)).thenReturn(List.of(incidenciaGuardada));

        PartidoIncidenciaRequestDTO request = new PartidoIncidenciaRequestDTO();
        request.setUsuarioId(2L);
        request.setTipoIncidencia("LESION");
        request.setSeveridad(3);
        request.setMinuto(35);
        request.setComentario("Lesion muscular");

        partidoService.registrarIncidencia(77L, owner, request);
        PartidoIncidenciaResumenDTO resumen = partidoService.obtenerResumenIncidencias(77L, owner);

        assertEquals(1, resumen.getTotalIncidencias());
        assertEquals(1, resumen.getLesiones());
        assertEquals("SEMI_ALTERADO", resumen.getEstadoCalidad());
        assertEquals(new BigDecimal("0.000"), resumen.getParticipacionVotacion());
    }

    private UsuarioEntity usuario(Long id, String nombre) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        return usuario;
    }
}
