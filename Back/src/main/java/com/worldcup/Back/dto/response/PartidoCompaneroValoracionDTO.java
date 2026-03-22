package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoCompaneroValoracionDTO {
    private Long jugadorId;
    private String jugadorNombre;
    private Integer puntuacion;
}
