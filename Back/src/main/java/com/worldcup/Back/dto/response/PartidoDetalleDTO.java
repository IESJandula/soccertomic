package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoDetalleDTO {
    private Long id;
    private UsuarioResumenDTO owner;
    private UsuarioResumenDTO creador;
    private List<PartidoOrganizadorDTO> organizadores;
    private LocalDateTime fecha;
    private String lugar;
    private Integer jugadoresPorEquipo;
    private String tipo;
    private String estado;
    private List<UsuarioResumenDTO> jugadoresInscritos;
    private List<UsuarioResumenDTO> equipoA;
    private List<UsuarioResumenDTO> equipoB;
    private Integer totalJugadores;
    private Boolean puedeInscribirse;
    private Boolean estaInscrito;
    private String colorEquipoA;
    private String colorEquipoB;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
