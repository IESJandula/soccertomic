package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerProfileRequestDTO {

    @NotNull(message = "Atributos de perfil requeridos")
    private AttributesDTO attributes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttributesDTO {

        @Min(value = 0, message = "Shooting debe estar entre 0 y 5")
        @Max(value = 5, message = "Shooting debe estar entre 0 y 5")
        private Integer shooting;

        @Min(value = 0, message = "Speed debe estar entre 0 y 5")
        @Max(value = 5, message = "Speed debe estar entre 0 y 5")
        private Integer speed;

        @Min(value = 0, message = "Dribbling debe estar entre 0 y 5")
        @Max(value = 5, message = "Dribbling debe estar entre 0 y 5")
        private Integer dribbling;

        @Min(value = 0, message = "Defense debe estar entre 0 y 5")
        @Max(value = 5, message = "Defense debe estar entre 0 y 5")
        private Integer defense;

        @Min(value = 0, message = "Strength debe estar entre 0 y 5")
        @Max(value = 5, message = "Strength debe estar entre 0 y 5")
        private Integer strength;

        @Min(value = 0, message = "Stamina debe estar entre 0 y 5")
        @Max(value = 5, message = "Stamina debe estar entre 0 y 5")
        private Integer stamina;

        @Min(value = 0, message = "Aerial debe estar entre 0 y 5")
        @Max(value = 5, message = "Aerial debe estar entre 0 y 5")
        private Integer aerial;

        private Boolean goalkeeper;

        private String posicionPreferida; // DELANTERO, MEDIOCAMPISTA, DEFENSA, PORTERO

        private String playStyle; // O, D, A

        private String skillTier; // BRONCE, PLATA, ORO, DIAMANTE

        private String ageRange; // UNDER_18, 18_25, 25_35, 35_50, OVER_50

        @Min(value = 1, message = "La autovaloración debe estar entre 1 y 5")
        @Max(value = 5, message = "La autovaloración debe estar entre 1 y 5")
        private Integer selfAssessment; // 1 (muy baja) a 5 (muy alta)

        private String piernaBuena; // Izquierda, Derecha, Ambidiestra

        private java.util.List<String> disponibilidad; // Array of availability options
    }
}

