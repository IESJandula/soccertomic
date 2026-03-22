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
public class PartidoResponseDTO {
    private Long id;
    private UsuarioResumenDTO owner;
    private UsuarioResumenDTO creador;
    private List<PartidoOrganizadorDTO> organizadores;
    private LocalDateTime fecha;
    private String lugar;
    private Integer jugadoresPorEquipo;
    private String tipo;
    private String estado;
    private Integer equipoACount;
    private Integer equipoBCount;
    private Integer totalJugadores;
    private String colorEquipoA;
    private String colorEquipoB;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
