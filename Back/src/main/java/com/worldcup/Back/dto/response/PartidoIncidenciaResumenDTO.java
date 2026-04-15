package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoIncidenciaResumenDTO {
    private Long partidoId;
    private Integer totalIncidencias;
    private Integer lesiones;
    private Integer abandonos;
    private Integer ausencias;
    private Integer conducta;
    private Integer otros;
    private String estadoCalidad;
    private BigDecimal scoreCalidad;
    private BigDecimal participacionVotacion;
}
