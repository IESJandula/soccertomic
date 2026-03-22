package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerProfileResponseDTO {

    private Long usuarioId;
    private AttributesDTO attributes;
    private LocalDateTime actualizadoEn;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttributesDTO {
        private Integer shooting;
        private Integer speed;
        private Integer dribbling;
        private Integer defense;
        private Integer strength;
        private Integer stamina;
        private Integer aerial;
        private Boolean goalkeeper;
        private String posicionPreferida;
        private String playStyle;
        private String skillTier;
        private String playTendency;
        private String ageRange;
        private Float globalRating;
        private String piernaBuena;
        private java.util.List<String> disponibilidad;
    }
}

