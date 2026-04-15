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
public class PartidoRatingProcesoResponseDTO {
    private Long partidoId;
    private Boolean yaProcesado;
    private Boolean procesado;
    private String version;
    private String resultadoResolucion;
    private Integer jugadoresActualizados;
    private Integer votosConsiderados;
    private Integer votosAtipicos;
    private String estadoCalidad;
    private String mensaje;
    private LocalDateTime procesadoEn;
}
