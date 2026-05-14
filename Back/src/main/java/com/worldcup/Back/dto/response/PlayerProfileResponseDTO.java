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
        private Boolean goalkeeper;
        private String posicionPreferida;
        private String playStyle;
        private String skillTier;
        private String playTendency;
        private String ageRange;
        private Integer selfAssessment;
        private String piernaBuena;
        private java.util.List<String> disponibilidad;
    }
}

