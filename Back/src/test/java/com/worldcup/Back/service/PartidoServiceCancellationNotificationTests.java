package com.worldcup.Back.service;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.EstadoPartido;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.repository.PartidoOrganizadorRepository;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceCancellationNotificationTests {

    @Mock
    private PartidoRepository partidoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PartidoOrganizadorRepository partidoOrganizadorRepository;

    @Mock
    private PartidoVotacionRepository partidoVotacionRepository;

    @Mock
    private PlayerProfileService playerProfileService;

    @Mock
    private TeamBalancingService teamBalancingService;

    @Mock
    private InvitacionService invitacionService;

    @InjectMocks
    private PartidoService partidoService;

    @Test
    void eliminarPartido_notificaATodasLasPersonasParticipantes_incluyendoQuienCancela() {
        UsuarioEntity owner = usuario(1L);
        UsuarioEntity participante = usuario(2L);
        UsuarioEntity equipo = usuario(3L);

        PartidoEntity partido = new PartidoEntity();
        partido.setId(99L);
        partido.setEstado(EstadoPartido.ABIERTO);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(owner, participante)));
        partido.setEquipoA(new ArrayList<>(List.of(equipo)));
        partido.setEquipoB(new ArrayList<>());

        PartidoOrganizadorEntity relacionOwner = new PartidoOrganizadorEntity();
        relacionOwner.setPartido(partido);
        relacionOwner.setUsuario(owner);
        relacionOwner.setRol(PartidoOrganizadorRol.OWNER);

        when(partidoRepository.findById(99L)).thenReturn(Optional.of(partido));
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner))
                .thenReturn(Optional.of(relacionOwner));
        when(partidoRepository.save(partido)).thenReturn(partido);

        partidoService.eliminarPartido(99L, owner);

        verify(invitacionService, times(1)).crearNotificacionCancelacion(partido, owner);
        verify(invitacionService, times(1)).crearNotificacionCancelacion(partido, participante);
        verify(invitacionService, times(1)).crearNotificacionCancelacion(partido, equipo);
        assertEquals(EstadoPartido.CANCELADO, partido.getEstado());
    }

    private UsuarioEntity usuario(Long id) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        return usuario;
    }
}
