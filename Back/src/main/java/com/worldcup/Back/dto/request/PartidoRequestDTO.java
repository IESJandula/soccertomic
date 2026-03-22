package com.worldcup.Back.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoRequestDTO {
    private LocalDateTime fecha;
    private String lugar;
    private Integer jugadoresPorEquipo; // 1-11
    private Integer duracionMinutos; // Default 60
    private String tipo; // PRIVADO o PUBLICO
    private String colorEquipoA; // Color del equipo A
    private String colorEquipoB; // Color del equipo B
}
