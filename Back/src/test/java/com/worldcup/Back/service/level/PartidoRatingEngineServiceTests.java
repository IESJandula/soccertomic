package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.PartidoCompaneroValoradoEmbeddable;
import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoVotacionEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.repository.PartidoIncidenciaRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoRatingEngineServiceTests {

    @Mock
    private PartidoVotacionRepository partidoVotacionRepository;

    @Mock
    private PartidoIncidenciaRepository partidoIncidenciaRepository;

    @InjectMocks
    private PartidoRatingEngineService partidoRatingEngineService;

    @Test
    void procesar_ponderaVotosPorFiabilidadParaResolverGanador() {
        UsuarioEntity equipoA = jugador(1L, "Equipo A", null);
        UsuarioEntity equipoB = jugador(2L, "Equipo B", null);

        UsuarioEntity votanteAltaFiabilidad = jugador(10L, "Votante Alta", new BigDecimal("100.00"));
        UsuarioEntity votanteBajaFiabilidad = jugador(11L, "Votante Baja", new BigDecimal("0.00"));

        PartidoEntity partido = partidoBase(List.of(equipoA), List.of(equipoB));

        PartidoVotacionEntity votoAlta = voto(partido, votanteAltaFiabilidad, 3, 0, List.of(), List.of());
        PartidoVotacionEntity votoBaja = voto(partido, votanteBajaFiabilidad, 0, 4, List.of(), List.of());

        when(partidoVotacionRepository.findByPartido(partido)).thenReturn(List.of(votoAlta, votoBaja));
        when(partidoIncidenciaRepository.findByPartidoOrderByCreadaEnDesc(partido)).thenReturn(List.of());

        PartidoRatingEngineService.EngineResult result = partidoRatingEngineService.procesar(partido);

        assertEquals("GANA_A", result.resultadoResolucion());
        assertEquals(2, result.jugadoresActualizados());
        assertEquals(2, result.votosConsiderados());
        assertEquals(0, result.votosAtipicos());
        assertEquals(1, equipoA.getVictorias());
        assertEquals(1, equipoB.getDerrotas());
    }

    @Test
    void procesar_detectaOutlierYNoLoConsideraEnElCalculo() {
        UsuarioEntity equipoA = jugador(1L, "Equipo A", null);
        UsuarioEntity equipoB = jugador(2L, "Equipo B", null);

        UsuarioEntity v1 = jugador(10L, "V1", new BigDecimal("50.00"));
        UsuarioEntity v2 = jugador(11L, "V2", new BigDecimal("50.00"));
        UsuarioEntity v3 = jugador(12L, "V3", new BigDecimal("50.00"));
        UsuarioEntity v4 = jugador(13L, "V4", new BigDecimal("50.00"));

        PartidoEntity partido = partidoBase(List.of(equipoA), List.of(equipoB));

        PartidoVotacionEntity voto1 = voto(partido, v1, 2, 1, List.of(), List.of());
        PartidoVotacionEntity voto2 = voto(partido, v2, 3, 2, List.of(), List.of());
        PartidoVotacionEntity voto3 = voto(partido, v3, 1, 0, List.of(), List.of());
        PartidoVotacionEntity votoOutlier = voto(partido, v4, 0, 8, List.of(), List.of());

        when(partidoVotacionRepository.findByPartido(partido)).thenReturn(List.of(voto1, voto2, voto3, votoOutlier));
        when(partidoIncidenciaRepository.findByPartidoOrderByCreadaEnDesc(partido)).thenReturn(List.of());

        PartidoRatingEngineService.EngineResult result = partidoRatingEngineService.procesar(partido);

        assertEquals("GANA_A", result.resultadoResolucion());
        assertEquals(3, result.votosConsiderados());
        assertEquals(1, result.votosAtipicos());
        assertEquals(1, v4.getVotosAtipicos());
        assertEquals(3, v1.getVotosValidos() + v2.getVotosValidos() + v3.getVotosValidos());
        assertTrue(result.usuariosActualizados().stream().anyMatch(u -> u.getId().equals(13L)));
    }

    @Test
    void procesar_aplicaSenialSocialAcotadaEnEmpate() {
        UsuarioEntity equipoA = jugador(1L, "Equipo A", null);
        UsuarioEntity equipoB = jugador(2L, "Equipo B", null);
        equipoA.setRatingMu(new BigDecimal("25.00"));
        equipoB.setRatingMu(new BigDecimal("25.00"));

        UsuarioEntity votante = jugador(10L, "Votante", new BigDecimal("80.00"));

        PartidoEntity partido = partidoBase(List.of(equipoA), List.of(equipoB));

        PartidoCompaneroValoradoEmbeddable actitudPositiva = new PartidoCompaneroValoradoEmbeddable(1L, 1);
        PartidoVotacionEntity voto = voto(partido, votante, 1, 1, List.of(1L), List.of(actitudPositiva));

        when(partidoVotacionRepository.findByPartido(partido)).thenReturn(List.of(voto));
        when(partidoIncidenciaRepository.findByPartidoOrderByCreadaEnDesc(partido)).thenReturn(List.of());

        PartidoRatingEngineService.EngineResult result = partidoRatingEngineService.procesar(partido);

        assertEquals("EMPATE", result.resultadoResolucion());
        assertEquals(new BigDecimal("25.02"), equipoA.getRatingMu());
        assertEquals(new BigDecimal("25.00"), equipoB.getRatingMu());
        assertEquals(1, result.votosConsiderados());
        assertEquals(0, result.votosAtipicos());
    }

    private PartidoEntity partidoBase(List<UsuarioEntity> equipoA, List<UsuarioEntity> equipoB) {
        PartidoEntity partido = new PartidoEntity();
        partido.setEquipoA(new ArrayList<>(equipoA));
        partido.setEquipoB(new ArrayList<>(equipoB));
        partido.setJugadoresInscritos(new ArrayList<>());
        partido.setScoreCalidad(new BigDecimal("1.000"));
        partido.setModoEquipos("MANUAL");
        partido.setParticipacionVotacion(new BigDecimal("0.000"));
        return partido;
    }

    private UsuarioEntity jugador(Long id, String nombre, BigDecimal fiabilidad) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        if (fiabilidad != null) {
            usuario.setFiabilidadScore(fiabilidad);
        }
        return usuario;
    }

    private PartidoVotacionEntity voto(PartidoEntity partido,
                                       UsuarioEntity votante,
                                       int golesA,
                                       int golesB,
                                       List<Long> diferenciales,
                                       List<PartidoCompaneroValoradoEmbeddable> valoraciones) {
        PartidoVotacionEntity voto = new PartidoVotacionEntity();
        voto.setPartido(partido);
        voto.setVotante(votante);
        voto.setGolesEquipoAPropuesto(golesA);
        voto.setGolesEquipoBPropuesto(golesB);
        voto.setIntensidadPartido("MEDIA");
        voto.setPartidoFueParejo(true);
        voto.setJugadoresDiferenciales(new ArrayList<>(diferenciales));
        voto.setValoracionesCompaneros(new ArrayList<>(valoraciones));
        return voto;
    }
}
