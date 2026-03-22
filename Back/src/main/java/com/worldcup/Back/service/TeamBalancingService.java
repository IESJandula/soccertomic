package com.worldcup.Back.service;

import com.worldcup.Back.entity.PartidoEntity;
import com.worldcup.Back.entity.UsuarioEntity;
import com.worldcup.Back.dto.response.BalanceResultDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TeamBalancer Service
 * 
 * Handles intelligent team distribution using multiple strategies:
 * - Snake draft (alternating picks by rating)
 * - Play style balancing (O/D/A distribution)
 * - Position-based distribution
 * - Availability consideration
 */
@Service
public class TeamBalancingService {

    /**
     * Assign positions to existing teams WITHOUT redistributing players
     * This method respects the current team assignments and only assigns field positions
     */
    public BalanceResultDTO asignarPosicionesSinRedistribuir(
            List<UsuarioEntity> equipoA,
            List<UsuarioEntity> equipoB,
            String formacion) {
        
        // Input validation
        if ((equipoA == null || equipoA.isEmpty()) && (equipoB == null || equipoB.isEmpty())) {
            throw new IllegalArgumentException("Se necesitan jugadores en al menos un equipo para asignar posiciones");
        }

        // Use existing teams (don't redistribute)
        List<UsuarioEntity> teamA = equipoA != null ? equipoA : new java.util.ArrayList<>();
        List<UsuarioEntity> teamB = equipoB != null ? equipoB : new java.util.ArrayList<>();

        // Step 1: Assign positions based on formation
        Map<UsuarioEntity, String> asignacionesEquipoAInternal = asignarPosiciones(teamA, formacion);
        Map<UsuarioEntity, String> asignacionesEquipoBInternal = asignarPosiciones(teamB, formacion);
        
        // Convert to Map<Long, String> for JSON serialization
        Map<Long, String> asignacionesEquipoA = convertirAsignacionesAIds(asignacionesEquipoAInternal);
        Map<Long, String> asignacionesEquipoB = convertirAsignacionesAIds(asignacionesEquipoBInternal);

        // Step 2: Evaluate balance
        BalanceMetrics metricas = evaluarBalance(teamA, teamB);

        // Build response
        BalanceResultDTO.BalanceMetricsDTO metricsDTO = BalanceResultDTO.BalanceMetricsDTO.builder()
                .promedioRatingEquipoA(metricas.getPromedioRatingEquipoA())
                .promedioRatingEquipoB(metricas.getPromedioRatingEquipoB())
                .diferenciaRating(metricas.getDiferenciaRating())
                .estiloEquipoA(metricas.getEstiloEquipoA())
                .estiloEquipoB(metricas.getEstiloEquipoB())
                .experienciaEquipoA(metricas.getExperienciaEquipoA())
                .experienciaEquipoB(metricas.getExperienciaEquipoB())
                .build();

        return BalanceResultDTO.builder()
                .equipoA(teamA)
                .equipoB(teamB)
                .asignacionesEquipoA(asignacionesEquipoA)
                .asignacionesEquipoB(asignacionesEquipoB)
                .metricas(metricsDTO)
                .balanceado(metricas.esBalanceado())
                .razonesDesbalance(metricas.getRazones())
                .build();
    }

    /**
     * Main balancing algorithm
     * Distributes players into two balanced teams using snake draft strategy
     */
    public BalanceResultDTO balancearEquipos(
            List<UsuarioEntity> jugadores,
            String formacion) {
        
        // Input validation - require at least 2 players
        if (jugadores == null || jugadores.size() < 2) {
            throw new IllegalArgumentException("Se necesitan al menos 2 jugadores para balancear equipos");
        }

        // Step 1: Sort by global rating (descending)
        List<UsuarioEntity> sortedByRating = procesarJugadores(jugadores);

        // Step 2: Apply snake draft
        SnakeDraftResult draftResult = aplicarSnakeDraft(sortedByRating);
        List<UsuarioEntity> equipoA = draftResult.equipoA;
        List<UsuarioEntity> equipoB = draftResult.equipoB;

        // Step 3: Refine distribution for balance (only if we have enough players)
        if (jugadores.size() >= 4) {
            refinamientoEquipos(equipoA, equipoB);
        }

        // Step 4: Assign positions based on formation
        Map<UsuarioEntity, String> asignacionesEquipoAInternal = asignarPosiciones(equipoA, formacion);
        Map<UsuarioEntity, String> asignacionesEquipoBInternal = asignarPosiciones(equipoB, formacion);
        
        // Convert to Map<Long, String> for JSON serialization
        Map<Long, String> asignacionesEquipoA = convertirAsignacionesAIds(asignacionesEquipoAInternal);
        Map<Long, String> asignacionesEquipoB = convertirAsignacionesAIds(asignacionesEquipoBInternal);

        // Step 5: Evaluate balance
        BalanceMetrics metricas = evaluarBalance(equipoA, equipoB);

        // Build response - convert BalanceMetrics to BalanceResultDTO.BalanceMetricsDTO
        BalanceResultDTO.BalanceMetricsDTO metricsDTO = BalanceResultDTO.BalanceMetricsDTO.builder()
                .promedioRatingEquipoA(metricas.getPromedioRatingEquipoA())
                .promedioRatingEquipoB(metricas.getPromedioRatingEquipoB())
                .diferenciaRating(metricas.getDiferenciaRating())
                .estiloEquipoA(metricas.getEstiloEquipoA())
                .estiloEquipoB(metricas.getEstiloEquipoB())
                .experienciaEquipoA(metricas.getExperienciaEquipoA())
                .experienciaEquipoB(metricas.getExperienciaEquipoB())
                .build();

        return BalanceResultDTO.builder()
                .equipoA(equipoA)
                .equipoB(equipoB)
                .asignacionesEquipoA(asignacionesEquipoA)
                .asignacionesEquipoB(asignacionesEquipoB)
                .metricas(metricsDTO)
                .balanceado(metricas.esBalanceado())
                .razonesDesbalance(metricas.getRazones())
                .build();
    }

    /**
     * Process and sort players by criteria
     * Players without PlayerProfile are assigned default rating of 3.0
     */
    private List<UsuarioEntity> procesarJugadores(List<UsuarioEntity> jugadores) {
        return jugadores.stream()
                .sorted((a, b) -> {
                    Float ratingA = (a.getPlayerProfile() != null && a.getPlayerProfile().getGlobalRating() != null)
                        ? a.getPlayerProfile().getGlobalRating() 
                        : 3.0f;
                    Float ratingB = (b.getPlayerProfile() != null && b.getPlayerProfile().getGlobalRating() != null)
                        ? b.getPlayerProfile().getGlobalRating() 
                        : 3.0f;
                    return ratingB.compareTo(ratingA); // Descending
                })
                .collect(Collectors.toList());
    }

    /**
     * Snake draft algorithm: alternate picking to balance teams
     * Team A: picks 1st, 3rd, 5th, 7th, ...
     * Team B: picks 2nd, 4th, 6th, 8th, ...
     */
    private SnakeDraftResult aplicarSnakeDraft(List<UsuarioEntity> jugadores) {
        List<UsuarioEntity> equipoA = new ArrayList<>();
        List<UsuarioEntity> equipoB = new ArrayList<>();

        for (int i = 0; i < jugadores.size(); i++) {
            if (i % 2 == 0) {
                equipoA.add(jugadores.get(i));
            } else {
                equipoB.add(jugadores.get(i));
            }
        }

        return new SnakeDraftResult(equipoA, equipoB);
    }

    /**
     * Refine team balance without breaking the 7-player limit
     * Only reorder if rating difference is significant
     */
    private void refinamientoEquipos(List<UsuarioEntity> equipoA, List<UsuarioEntity> equipoB) {
        Float avgA = calcularPromedioRating(equipoA);
        Float avgB = calcularPromedioRating(equipoB);
        Float diferencia = Math.abs(avgA - avgB);

        // If difference > 0.5, try to swap similar-rated players
        if (diferencia > 0.5) {
            boolean improved = true;
            while (improved) {
                improved = false;
                for (int i = 0; i < equipoA.size(); i++) {
                    for (int j = 0; j < equipoB.size(); j++) {
                        Float ratingAi = getRating(equipoA.get(i));
                        Float ratingBj = getRating(equipoB.get(j));

                        // Calculate balance after swap
                        Float newAvgA = (avgA - ratingAi + ratingBj) / equipoA.size();
                        Float newAvgB = (avgB - ratingBj + ratingAi) / equipoB.size();
                        Float newDiferencia = Math.abs(newAvgA - newAvgB);

                        // If swap improves balance, do it
                        if (newDiferencia < diferencia) {
                            // Perform actual swap between teams
                            UsuarioEntity playerA = equipoA.get(i);
                            UsuarioEntity playerB = equipoB.get(j);
                            equipoA.set(i, playerB);
                            equipoB.set(j, playerA);
                            
                            avgA = newAvgA;
                            avgB = newAvgB;
                            diferencia = newDiferencia;
                            improved = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Assign players to positions based on formation and attributes
     */
    private Map<UsuarioEntity, String> asignarPosiciones(
            List<UsuarioEntity> equipo,
            String formacion) {

        Map<UsuarioEntity, String> asignaciones = new HashMap<>();

        // Get position slots for formation
        List<String> posiciones = obtenerPosiciones(formacion);

        // Sort players for position assignment
        List<UsuarioEntity> jugadoresOrdenados = equipo.stream()
                .sorted((a, b) -> compararJugadoresParaPosicion(a, b))
                .collect(Collectors.toList());

        // Assign positions
        for (int i = 0; i < Math.min(jugadoresOrdenados.size(), posiciones.size()); i++) {
            asignaciones.put(jugadoresOrdenados.get(i), posiciones.get(i));
        }

        return asignaciones;
    }

    /**
     * Convert position assignments from Map<UsuarioEntity, String> to Map<Long, String>
     * This is needed for proper JSON serialization where player IDs are used as keys
     */
    private Map<Long, String> convertirAsignacionesAIds(Map<UsuarioEntity, String> asignaciones) {
        Map<Long, String> asignacionesPorId = new HashMap<>();
        for (Map.Entry<UsuarioEntity, String> entry : asignaciones.entrySet()) {
            asignacionesPorId.put(entry.getKey().getId(), entry.getValue());
        }
        return asignacionesPorId;
    }

    /**
     * Get position slots for a given formation
     * Formation codes: 1-2-2-1-1, 1-3-2-1, etc.
     */
    private List<String> obtenerPosiciones(String formacion) {
        List<String> posiciones = new ArrayList<>();

        // GK (1)
        posiciones.add("GK");

        if ("formation-2".equals(formacion)) {
            // 1-3-2-1: 3 defensas
            posiciones.add("DF");
            posiciones.add("DF");
            posiciones.add("DF");
            // 2 mediocampistas
            posiciones.add("MF");
            posiciones.add("MF");
            // 1 delantero
            posiciones.add("FW");
        } else {
            // formation-1 (1-2-2-1-1): 2 defensas, 2 extremos, 1 mediocentro, 1 delantero
            posiciones.add("DF");
            posiciones.add("DF");
            // 2 wingers
            posiciones.add("WG");
            posiciones.add("WG");
            // 1 midfielder
            posiciones.add("MF");
            // 1 forward
            posiciones.add("FW");
        }

        return posiciones;
    }

    /**
     * Compare two players for position suitability
     * Primary: defenders (high defense), forwards (high shooting)
     * Secondary: balance with play style (O/D/A)
     */
    private int compararJugadoresParaPosicion(UsuarioEntity a, UsuarioEntity b) {
        Integer defensaA = (a.getPlayerProfile() != null && a.getPlayerProfile().getDefense() != null) 
            ? a.getPlayerProfile().getDefense() : 3;
        Integer defensaB = (b.getPlayerProfile() != null && b.getPlayerProfile().getDefense() != null) 
            ? b.getPlayerProfile().getDefense() : 3;
        Integer disparoA = (a.getPlayerProfile() != null && a.getPlayerProfile().getShooting() != null) 
            ? a.getPlayerProfile().getShooting() : 3;
        Integer disparoB = (b.getPlayerProfile() != null && b.getPlayerProfile().getShooting() != null) 
            ? b.getPlayerProfile().getShooting() : 3;

        // Defenders should have high defense
        if (!defensaA.equals(defensaB)) {
            return defensaB.compareTo(defensaA); // Descending
        }

        // Forwards should have high shooting
        if (!disparoA.equals(disparoB)) {
            return disparoB.compareTo(disparoA); // Descending
        }

        return 0;
    }

    /**
     * Evaluate overall balance of the teams
     */
    private BalanceMetrics evaluarBalance(List<UsuarioEntity> equipoA, List<UsuarioEntity> equipoB) {
        BalanceMetrics metricas = new BalanceMetrics();

        // Rating balance
        Float avgA = calcularPromedioRating(equipoA);
        Float avgB = calcularPromedioRating(equipoB);
        Float diferenciaRating = Math.abs(avgA - avgB);

        metricas.setPromedioRatingEquipoA(avgA);
        metricas.setPromedioRatingEquipoB(avgB);
        metricas.setDiferenciaRating(diferenciaRating);

        if (diferenciaRating > 0.5) {
            metricas.agregarRazon(
                String.format("Diferencia de rating: %.1f (Team A: %.1f vs Team B: %.1f)", 
                    diferenciaRating, avgA, avgB)
            );
        }

        // Play style balance
        evaluarBalanceEstilo(equipoA, equipoB, metricas);

        // Experience level balance
        evaluarBalanceExperiencia(equipoA, equipoB, metricas);

        // Calculate availability matching
        evaluarDisponibilidad(equipoA, equipoB, metricas);

        return metricas;
    }

    /**
     * Evaluate play style (O/D/A) distribution
     */
    private void evaluarBalanceEstilo(
            List<UsuarioEntity> equipoA,
            List<UsuarioEntity> equipoB,
            BalanceMetrics metricas) {

        Map<String, Long> estiloA = contarEstilos(equipoA);
        Map<String, Long> estiloB = contarEstilos(equipoB);

        long ofensivosA = estiloA.getOrDefault("O", 0L);
        long ofensivosB = estiloB.getOrDefault("O", 0L);
        long defensivosA = estiloA.getOrDefault("D", 0L);
        long defensivosB = estiloB.getOrDefault("D", 0L);

        if (Math.abs(ofensivosA - ofensivosB) > 2) {
            metricas.agregarRazon(
                String.format("Desbalance de jugadores ofensivos: %d vs %d", ofensivosA, ofensivosB)
            );
        }

        if (Math.abs(defensivosA - defensivosB) > 2) {
            metricas.agregarRazon(
                String.format("Desbalance de jugadores defensivos: %d vs %d", defensivosA, defensivosB)
            );
        }

        metricas.setEstiloEquipoA(estiloA);
        metricas.setEstiloEquipoB(estiloB);
    }

    /**
     * Evaluate experience level distribution
     */
    private void evaluarBalanceExperiencia(
            List<UsuarioEntity> equipoA,
            List<UsuarioEntity> equipoB,
            BalanceMetrics metricas) {

        Map<String, Long> experienciaA = contarExperiencia(equipoA);
        Map<String, Long> experienciaB = contarExperiencia(equipoB);

        long avanzadosA = experienciaA.getOrDefault("advanced", 0L);
        long avanzadosB = experienciaB.getOrDefault("advanced", 0L);

        if (avanzadosA > avanzadosB + 2 || avanzadosB > avanzadosA + 2) {
            metricas.agregarRazon("Desbalance de jugadores experimentados");
        }

        metricas.setExperienciaEquipoA(experienciaA);
        metricas.setExperienciaEquipoB(experienciaB);
    }

    /**
     * Evaluate if teams have complementary availability schedules
     */
    private void evaluarDisponibilidad(
            List<UsuarioEntity> equipoA,
            List<UsuarioEntity> equipoB,
            BalanceMetrics metricas) {

        // Count players with matching availability in each team
        long disponiblesA = equipoA.stream()
                .filter(u -> u.getPlayerProfile() != null && 
                            u.getPlayerProfile().getDisponibilidad() != null &&
                            !u.getPlayerProfile().getDisponibilidad().isEmpty())
                .count();

        long disponiblesB = equipoB.stream()
                .filter(u -> u.getPlayerProfile() != null && 
                            u.getPlayerProfile().getDisponibilidad() != null &&
                            !u.getPlayerProfile().getDisponibilidad().isEmpty())
                .count();

        if (Math.abs(disponiblesA - disponiblesB) > 2) {
            metricas.agregarRazon("Diferencia en disponibilidad de horarios entre equipos");
        }
    }

    /**
     * Count play styles in a team
     */
    private Map<String, Long> contarEstilos(List<UsuarioEntity> equipo) {
        return equipo.stream()
                .collect(Collectors.groupingBy(
                    u -> (u.getPlayerProfile() != null && u.getPlayerProfile().getPlayStyle() != null)
                        ? u.getPlayerProfile().getPlayStyle() 
                        : "A",
                    Collectors.counting()
                ));
    }

    /**
     * Count experience levels in a team
     */
    private Map<String, Long> contarExperiencia(List<UsuarioEntity> equipo) {
        return equipo.stream()
                .collect(Collectors.groupingBy(
                    u -> (u.getPlayerProfile() != null && u.getPlayerProfile().getAgeRange() != null)
                        ? u.getPlayerProfile().getAgeRange()
                        : "18_25",
                    Collectors.counting()
                ));
    }

    /**
     * Calculate average rating of team
     */
    private Float calcularPromedioRating(List<UsuarioEntity> equipo) {
        if (equipo.isEmpty()) return 0f;

        return (float) equipo.stream()
                .mapToDouble(u -> getRating(u))
                .average()
                .orElse(3.0);
    }

    /**
     * Get rating from user, with fallback to 3.0
     */
    private Float getRating(UsuarioEntity usuario) {
        if (usuario.getPlayerProfile() != null && usuario.getPlayerProfile().getGlobalRating() != null) {
            return usuario.getPlayerProfile().getGlobalRating();
        }
        return 3.0f;
    }

    // ==================== Helper Classes ====================

    @Data
    @AllArgsConstructor
    public static class SnakeDraftResult {
        private List<UsuarioEntity> equipoA;
        private List<UsuarioEntity> equipoB;
    }

    /**
     * Balance metrics and analysis
     */
    @Data
    public static class BalanceMetrics {
        private Float promedioRatingEquipoA;
        private Float promedioRatingEquipoB;
        private Float diferenciaRating;
        private Map<String, Long> estiloEquipoA;
        private Map<String, Long> estiloEquipoB;
        private Map<String, Long> experienciaEquipoA;
        private Map<String, Long> experienciaEquipoB;
        private List<String> razones = new ArrayList<>();

        public void agregarRazon(String razon) {
            this.razones.add(razon);
        }

        public boolean esBalanceado() {
            return this.razones.isEmpty() || 
                   (this.razones.size() == 1 && this.diferenciaRating < 0.3);
        }
    }
}
