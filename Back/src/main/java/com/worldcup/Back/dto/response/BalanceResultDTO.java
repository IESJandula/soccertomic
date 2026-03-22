package com.worldcup.Back.dto.response;

import com.worldcup.Back.entity.UsuarioEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * BalanceResultDTO
 * 
 * Response DTO for team balancing operations
 * Contains distributed teams, position assignments, and balance metrics
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResultDTO {

    /**
     * Team A players (distributed)
     */
    private List<UsuarioEntity> equipoA;

    /**
     * Team B players (distributed)
     */
    private List<UsuarioEntity> equipoB;

    /**
     * Position assignments for Team A
     * Map<PlayerId, PositionCode> e.g. "GK", "DF", "MF", "WG", "FW"
     */
    private Map<Long, String> asignacionesEquipoA;

    /**
     * Position assignments for Team B
     */
    private Map<Long, String> asignacionesEquipoB;

    /**
     * Balance metrics and analysis
     */
    private BalanceMetricsDTO metricas;

    /**
     * Is the balance acceptable?
     */
    private boolean balanceado;

    /**
     * Reasons why balance may not be perfect
     * Empty list if balanceado = true
     */
    private List<String> razonesDesbalance;

    /**
     * Nested DTO for balance metrics
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BalanceMetricsDTO {
        private Float promedioRatingEquipoA;
        private Float promedioRatingEquipoB;
        private Float diferenciaRating;

        /**
         * Play style distribution (O, D, A counts)
         */
        private Map<String, Long> estiloEquipoA;
        private Map<String, Long> estiloEquipoB;

        /**
         * Experience level distribution (beginner, intermediate, advanced counts)
         */
        private Map<String, Long> experienciaEquipoA;
        private Map<String, Long> experienciaEquipoB;
    }
}
