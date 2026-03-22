package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoVotacionPanelCompartidoDTO {
    private Long partidoId;
    private Integer totalVotos;
    private Double promedioGolesEquipoA;
    private Double promedioGolesEquipoB;
    private Double porcentajePartidoParejo;
    private String intensidadMasVotada;
    private List<JugadorConteoDTO> jugadoresDiferenciales;
    private List<JugadorPromedioDTO> valoracionCompaneros;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JugadorConteoDTO {
        private Long jugadorId;
        private String jugadorNombre;
        private Integer votos;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JugadorPromedioDTO {
        private Long jugadorId;
        private String jugadorNombre;
        private Double promedio;
        private Integer cantidadVotos;
        private Integer votosPositivos;
        private Integer votosNegativos;
    }
}