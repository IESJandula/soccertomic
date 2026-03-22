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

        @NotNull(message = "Shooting requerido")
        @Min(value = 0, message = "Shooting debe estar entre 0 y 5")
        @Max(value = 5, message = "Shooting debe estar entre 0 y 5")
        private Integer shooting;

        @NotNull(message = "Speed requerido")
        @Min(value = 0, message = "Speed debe estar entre 0 y 5")
        @Max(value = 5, message = "Speed debe estar entre 0 y 5")
        private Integer speed;

        @NotNull(message = "Dribbling requerido")
        @Min(value = 0, message = "Dribbling debe estar entre 0 y 5")
        @Max(value = 5, message = "Dribbling debe estar entre 0 y 5")
        private Integer dribbling;

        @NotNull(message = "Defense requerido")
        @Min(value = 0, message = "Defense debe estar entre 0 y 5")
        @Max(value = 5, message = "Defense debe estar entre 0 y 5")
        private Integer defense;

        @NotNull(message = "Strength requerido")
        @Min(value = 0, message = "Strength debe estar entre 0 y 5")
        @Max(value = 5, message = "Strength debe estar entre 0 y 5")
        private Integer strength;

        @NotNull(message = "Stamina requerido")
        @Min(value = 0, message = "Stamina debe estar entre 0 y 5")
        @Max(value = 5, message = "Stamina debe estar entre 0 y 5")
        private Integer stamina;

        @NotNull(message = "Aerial requerido")
        @Min(value = 0, message = "Aerial debe estar entre 0 y 5")
        @Max(value = 5, message = "Aerial debe estar entre 0 y 5")
        private Integer aerial;

        @NotNull(message = "Goalkeeper requerido")
        private Boolean goalkeeper;

        @NotNull(message = "Posición preferida requerida")
        private String posicionPreferida; // DELANTERO, MEDIOCAMPISTA, DEFENSA, PORTERO

        @NotNull(message = "PlayStyle requerido")
        private String playStyle; // O, D, A

        @NotNull(message = "SkillTier requerido")
        private String skillTier; // BRONCE, PLATA, ORO, DIAMANTE

        @NotNull(message = "Rango de edad requerido")
        private String ageRange; // UNDER_18, 18_25, 25_35, 35_50, OVER_50

        private String piernaBuena; // Izquierda, Derecha, Ambidiestra

        private java.util.List<String> disponibilidad; // Array of availability options
    }
}

