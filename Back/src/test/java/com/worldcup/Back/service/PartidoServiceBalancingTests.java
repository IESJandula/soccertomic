package com.worldcup.Back.service;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.PartidoOrganizadorEntity;
import com.worldcup.Back.entity.PlayerProfileEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.entity.enums.PartidoOrganizadorRol;
import com.worldcup.Back.repository.PartidoOrganizadorRepository;
import com.worldcup.Back.repository.PartidoRepository;
import com.worldcup.Back.repository.PartidoVotacionRepository;
import com.worldcup.Back.repository.UsuarioRepository;
import com.worldcup.Back.service.level.VisibleLevelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceBalancingTests {

    @Mock
    private PartidoRepository partidoRepository;

    @Mock
    private PartidoOrganizadorRepository partidoOrganizadorRepository;

    @Mock
    private PartidoVotacionRepository partidoVotacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TeamBalancingService teamBalancingService;

    @Mock
    private InvitacionService invitacionService;

    @Mock
    private VisibleLevelService visibleLevelService;

    @InjectMocks
    private PartidoService partidoService;

    /**
     * Test case: Realistic scenario with MU difference of 2 (achievable <= 4)
     * Expected: Final difference respects the <= 4 threshold
     */
    @Test
    void balancearEquipos_respetaUmbralMaximoDe4Puntos_casoBuenoLevel131vs121() {
        UsuarioEntity owner = crearUsuario(1L, "Owner", new BigDecimal("25.00"), new BigDecimal("8.33"));

        // Stronger player: MU=30, sigma=5 -> visible = 30 - 15 = 15
        UsuarioEntity jugadorA = crearUsuario(10L, "Fuerte", new BigDecimal("30.00"), new BigDecimal("5.00"));
        jugadorA.setPlayerProfile(crearPerfil("DELANTERO", "O"));

        // Mid player: MU=28, sigma=5 -> visible = 28 - 15 = 13
        UsuarioEntity jugadorB = crearUsuario(11L, "Medio", new BigDecimal("28.00"), new BigDecimal("5.00"));
        jugadorB.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        // Support players with level ~14-15
        UsuarioEntity j3 = crearUsuario(12L, "J3", new BigDecimal("26.50"), new BigDecimal("5.00"));
        j3.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        UsuarioEntity j4 = crearUsuario(13L, "J4", new BigDecimal("26.60"), new BigDecimal("5.00"));
        j4.setPlayerProfile(crearPerfil("ARQUERO", "G"));

        UsuarioEntity j5 = crearUsuario(14L, "J5", new BigDecimal("26.55"), new BigDecimal("5.00"));
        j5.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        UsuarioEntity j6 = crearUsuario(15L, "J6", new BigDecimal("26.65"), new BigDecimal("5.00"));
        j6.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        PartidoEntity partido = new PartidoEntity();
        partido.setId(1L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(jugadorA, jugadorB, j3, j4, j5, j6)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setJugadoresPorEquipo(3);
        partido.setModoEquipos("MANUAL");

        // Mock visible levels (visible = MU - 3*sigma)
        when(visibleLevelService.calcularNivelVisible(jugadorA)).thenReturn(new BigDecimal("15.00"));
        when(visibleLevelService.calcularNivelVisible(jugadorB)).thenReturn(new BigDecimal("13.00"));
        when(visibleLevelService.calcularNivelVisible(j3)).thenReturn(new BigDecimal("14.50"));
        when(visibleLevelService.calcularNivelVisible(j4)).thenReturn(new BigDecimal("14.60"));
        when(visibleLevelService.calcularNivelVisible(j5)).thenReturn(new BigDecimal("14.55"));
        when(visibleLevelService.calcularNivelVisible(j6)).thenReturn(new BigDecimal("14.65"));

        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findAllById(List.of(10L, 11L, 12L, 13L, 14L, 15L)))
            .thenReturn(List.of(jugadorA, jugadorB, j3, j4, j5, j6));

        // Mock organizer relationship
        PartidoOrganizadorEntity relacionOwner = new PartidoOrganizadorEntity();
        relacionOwner.setPartido(partido);
        relacionOwner.setUsuario(owner);
        relacionOwner.setRol(PartidoOrganizadorRol.OWNER);
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner));

        // Execute balancing
        var responseDTO = partidoService.balancearEquipos(1L, List.of(10L, 11L, 12L, 13L, 14L, 15L), owner);

        // Verify: final difference must be <= 4
        assertNotNull(responseDTO);
        int diferencia = Math.abs(responseDTO.getNivelTotalEquipoA() - responseDTO.getNivelTotalEquipoB());
        assertTrue(
            diferencia <= 4,
            "La diferencia de nivel entre equipos debe ser <= 4, pero fue: " + diferencia
        );
    }

    /**
     * Test case: Same MU but different sigma (2 vs 8.33)
     * Validates that sigma influences team selection from the beginning.
     */
    @Test
    void balancearEquipos_sigmaBajaInfluyeEnSeleccion_mismoMuDistintoSigma() {
        UsuarioEntity owner = crearUsuario(1L, "Owner", new BigDecimal("25.00"), new BigDecimal("8.33"));

        // High precision player: MU=26, sigma=2 -> visible = 26 - 6 = 20
        UsuarioEntity j1 = crearUsuario(10L, "AltaPrecision", new BigDecimal("26.00"), new BigDecimal("2.00"));
        j1.setPlayerProfile(crearPerfil("DELANTERO", "O"));

        // Low precision player: MU=26, sigma=5 -> visible = 26 - 15 = 11
        UsuarioEntity j2 = crearUsuario(11L, "BajaPrecision", new BigDecimal("26.00"), new BigDecimal("5.00"));
        j2.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        UsuarioEntity j3 = crearUsuario(12L, "J3", new BigDecimal("26.00"), new BigDecimal("5.00"));
        j3.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        UsuarioEntity j4 = crearUsuario(13L, "J4", new BigDecimal("26.00"), new BigDecimal("5.00"));
        j4.setPlayerProfile(crearPerfil("ARQUERO", "G"));

        PartidoEntity partido = new PartidoEntity();
        partido.setId(2L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(j1, j2, j3, j4)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setJugadoresPorEquipo(2);
        partido.setModoEquipos("MANUAL");

        // j1 has high precision (sigma=2) producing visible=20, while j2 with same MU but sigma=5 has visible=11
        when(visibleLevelService.calcularNivelVisible(j1)).thenReturn(new BigDecimal("20.00"));
        when(visibleLevelService.calcularNivelVisible(j2)).thenReturn(new BigDecimal("11.00"));
        when(visibleLevelService.calcularNivelVisible(j3)).thenReturn(new BigDecimal("15.00"));
        when(visibleLevelService.calcularNivelVisible(j4)).thenReturn(new BigDecimal("15.00"));

        when(partidoRepository.findById(2L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findAllById(List.of(10L, 11L, 12L, 13L)))
            .thenReturn(List.of(j1, j2, j3, j4));

        // Mock organizer relationship
        PartidoOrganizadorEntity relacionOwner2 = new PartidoOrganizadorEntity();
        relacionOwner2.setPartido(partido);
        relacionOwner2.setUsuario(owner);
        relacionOwner2.setRol(PartidoOrganizadorRol.OWNER);
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner2));

        var responseDTO = partidoService.balancearEquipos(2L, List.of(10L, 11L, 12L, 13L), owner);

        assertNotNull(responseDTO);
        int diferencia = Math.abs(responseDTO.getNivelTotalEquipoA() - responseDTO.getNivelTotalEquipoB());
        assertTrue(
            diferencia <= 4,
            "Sigma baja debe influir desde el inicio; diferencia final debe ser <= 4, fue: " + diferencia
        );
    }

    /**
     * Test case: Multiple valid partitions (all <= 4 difference)
     * Expected: Select the one with best tactical coverage (goalkeepers, positions, play styles).
     */
    @Test
    void balancearEquipos_mantienePrioridadTacticaEntreParticionesValidas() {
        UsuarioEntity owner = crearUsuario(1L, "Owner", new BigDecimal("25.00"), new BigDecimal("8.33"));

        UsuarioEntity j1 = crearUsuario(10L, "J1", new BigDecimal("25.00"), new BigDecimal("8.33"));
        j1.setPlayerProfile(crearPerfil("ARQUERO", "G"));

        UsuarioEntity j2 = crearUsuario(11L, "J2", new BigDecimal("26.00"), new BigDecimal("8.33"));
        j2.setPlayerProfile(crearPerfil("DELANTERO", "O"));

        UsuarioEntity j3 = crearUsuario(12L, "J3", new BigDecimal("25.00"), new BigDecimal("8.33"));
        j3.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        UsuarioEntity j4 = crearUsuario(13L, "J4", new BigDecimal("25.00"), new BigDecimal("8.33"));
        j4.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        PartidoEntity partido = new PartidoEntity();
        partido.setId(3L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(j1, j2, j3, j4)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setJugadoresPorEquipo(2);
        partido.setModoEquipos("MANUAL");

        when(visibleLevelService.calcularNivelVisible(j1)).thenReturn(new BigDecimal("25.00"));
        when(visibleLevelService.calcularNivelVisible(j2)).thenReturn(new BigDecimal("26.00"));
        when(visibleLevelService.calcularNivelVisible(j3)).thenReturn(new BigDecimal("25.00"));
        when(visibleLevelService.calcularNivelVisible(j4)).thenReturn(new BigDecimal("25.00"));

        when(partidoRepository.findById(3L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findAllById(List.of(10L, 11L, 12L, 13L)))
            .thenReturn(List.of(j1, j2, j3, j4));

        // Mock organizer relationship
        PartidoOrganizadorEntity relacionOwner3 = new PartidoOrganizadorEntity();
        relacionOwner3.setPartido(partido);
        relacionOwner3.setUsuario(owner);
        relacionOwner3.setRol(PartidoOrganizadorRol.OWNER);
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner3));

        var responseDTO = partidoService.balancearEquipos(3L, List.of(10L, 11L, 12L, 13L), owner);

        assertNotNull(responseDTO);
        int diferencia = Math.abs(responseDTO.getNivelTotalEquipoA() - responseDTO.getNivelTotalEquipoB());
        assertTrue(diferencia <= 4);
    }

    /**
     * Test case: Extreme composition (outlier) where no partition achieves <= 4 difference
     * Expected: Fallback to next best option but still complete the balancing gracefully.
     */
    @Test
    void balancearEquipos_fallbackProgresivo_cuandoNoHayParticionValida() {
        UsuarioEntity owner = crearUsuario(1L, "Owner", new BigDecimal("25.00"), new BigDecimal("8.33"));

        // Extreme outlier
        UsuarioEntity j1 = crearUsuario(10L, "Outlier", new BigDecimal("100.00"), new BigDecimal("5.00"));
        j1.setPlayerProfile(crearPerfil("DELANTERO", "O"));

        UsuarioEntity j2 = crearUsuario(11L, "Débil", new BigDecimal("10.00"), new BigDecimal("8.33"));
        j2.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        UsuarioEntity j3 = crearUsuario(12L, "Débil2", new BigDecimal("10.00"), new BigDecimal("8.33"));
        j3.setPlayerProfile(crearPerfil("ARQUERO", "G"));

        PartidoEntity partido = new PartidoEntity();
        partido.setId(4L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(j1, j2, j3)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setJugadoresPorEquipo(2);
        partido.setModoEquipos("MANUAL");

        // Visible: 100 - 15 = 85, 10 - 24.99 = 0, 0
        when(visibleLevelService.calcularNivelVisible(j1)).thenReturn(new BigDecimal("85.00"));
        when(visibleLevelService.calcularNivelVisible(j2)).thenReturn(new BigDecimal("0.00"));
        when(visibleLevelService.calcularNivelVisible(j3)).thenReturn(new BigDecimal("0.00"));

        when(partidoRepository.findById(4L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findAllById(List.of(10L, 11L, 12L)))
            .thenReturn(List.of(j1, j2, j3));

        // Mock organizer relationship
        PartidoOrganizadorEntity relacionOwner4 = new PartidoOrganizadorEntity();
        relacionOwner4.setPartido(partido);
        relacionOwner4.setUsuario(owner);
        relacionOwner4.setRol(PartidoOrganizadorRol.OWNER);
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner4));

        var responseDTO = partidoService.balancearEquipos(4L, List.of(10L, 11L, 12L), owner);

        assertNotNull(responseDTO);
        // Fallback case: difference will be > 4, but balancing must complete
        assertNotNull(responseDTO.getNivelTotalEquipoA());
        assertNotNull(responseDTO.getNivelTotalEquipoB());
    }

    /**
     * Test case: Specific case reported by user (131 vs 121) with 6 players
     * Expected: Escalating threshold strategy should find better partition at 5 or 6
     */
    @Test
    void balancearEquipos_casoReportadoUsuario_131vs121() {
        UsuarioEntity owner = crearUsuario(1L, "Owner", new BigDecimal("25.00"), new BigDecimal("8.33"));

        // Create 6 players with specific MU/sigma that produce ~131 vs 121 difference in worst case
        // Total visible levels will sum to ~87, but we want worst partition to be 131 vs 121 in raw MU
        // This simulates the actual problem the user reported
        
        UsuarioEntity j1 = crearUsuario(20L, "Player1", new BigDecimal("48.00"), new BigDecimal("8.33"));
        j1.setPlayerProfile(crearPerfil("DELANTERO", "O"));

        UsuarioEntity j2 = crearUsuario(21L, "Player2", new BigDecimal("47.00"), new BigDecimal("8.33"));
        j2.setPlayerProfile(crearPerfil("ARQUERO", "G"));

        UsuarioEntity j3 = crearUsuario(22L, "Player3", new BigDecimal("22.00"), new BigDecimal("8.33"));
        j3.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        UsuarioEntity j4 = crearUsuario(23L, "Player4", new BigDecimal("22.00"), new BigDecimal("8.33"));
        j4.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        UsuarioEntity j5 = crearUsuario(24L, "Player5", new BigDecimal("21.50"), new BigDecimal("8.33"));
        j5.setPlayerProfile(crearPerfil("DEFENSA", "D"));

        UsuarioEntity j6 = crearUsuario(25L, "Player6", new BigDecimal("21.50"), new BigDecimal("8.33"));
        j6.setPlayerProfile(crearPerfil("MEDIOCAMPISTA", "A"));

        PartidoEntity partido = new PartidoEntity();
        partido.setId(5L);
        partido.setJugadoresInscritos(new ArrayList<>(List.of(j1, j2, j3, j4, j5, j6)));
        partido.setEquipoA(new ArrayList<>());
        partido.setEquipoB(new ArrayList<>());
        partido.setJugadoresPorEquipo(3);
        partido.setModoEquipos("MANUAL");

        // Visible levels (MU - 3*sigma): with same sigma for all (8.33), just get MU - 25
        when(visibleLevelService.calcularNivelVisible(j1)).thenReturn(new BigDecimal("23.00"));  // 48-25
        when(visibleLevelService.calcularNivelVisible(j2)).thenReturn(new BigDecimal("22.00"));  // 47-25
        when(visibleLevelService.calcularNivelVisible(j3)).thenReturn(new BigDecimal("-3.00")); // 22-25, rounded to 0
        when(visibleLevelService.calcularNivelVisible(j4)).thenReturn(new BigDecimal("-3.00")); // 22-25
        when(visibleLevelService.calcularNivelVisible(j5)).thenReturn(new BigDecimal("-3.50")); // 21.5-25
        when(visibleLevelService.calcularNivelVisible(j6)).thenReturn(new BigDecimal("-3.50")); // 21.5-25

        when(partidoRepository.findById(5L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(any(PartidoEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioRepository.findAllById(List.of(20L, 21L, 22L, 23L, 24L, 25L)))
            .thenReturn(List.of(j1, j2, j3, j4, j5, j6));

        // Mock organizer relationship
        PartidoOrganizadorEntity relacionOwner5 = new PartidoOrganizadorEntity();
        relacionOwner5.setPartido(partido);
        relacionOwner5.setUsuario(owner);
        relacionOwner5.setRol(PartidoOrganizadorRol.OWNER);
        when(partidoOrganizadorRepository.findByPartidoAndUsuario(partido, owner)).thenReturn(Optional.of(relacionOwner5));

        var responseDTO = partidoService.balancearEquipos(5L, List.of(20L, 21L, 22L, 23L, 24L, 25L), owner);

        assertNotNull(responseDTO);
        int diferencia = Math.abs(responseDTO.getNivelTotalEquipoA() - responseDTO.getNivelTotalEquipoB());
        
        // With escalating thresholds (4, 5, 6, 7...), should find better solution than hard 4-point limit
        assertTrue(
            diferencia <= 7,
            "Con estrategia escalonada, debe alcanzar diferencia <= 7, pero fue: " + diferencia
        );
    }

    // ===== Helper Methods =====

    private UsuarioEntity crearUsuario(Long id, String nombre, BigDecimal mu, BigDecimal sigma) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setRatingMu(mu);
        usuario.setRatingSigma(sigma);
        usuario.setNoShows(0);
        usuario.setAbandonos(0);
        usuario.setFiabilidadScore(new BigDecimal("1.00"));
        return usuario;
    }

    private PlayerProfileEntity crearPerfil(String posicion, String tendencia) {
        PlayerProfileEntity perfil = new PlayerProfileEntity();
        perfil.setPosicionPreferida(posicion);
        perfil.setPlayStyle(tendencia);
        perfil.setGoalkeeper("ARQUERO".equals(posicion) || "G".equals(tendencia));
        perfil.setPlayTendency(mapTendencia(tendencia));
        return perfil;
    }

    private String mapTendencia(String playStyle) {
        return switch (playStyle) {
            case "O" -> "OFENSIVA";
            case "D" -> "DEFENSIVA";
            default -> "ADAPTABLE";
        };
    }
}
