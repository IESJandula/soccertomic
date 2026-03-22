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
public class EquipoRapidoResponseDTO {
    private Long id;
    private String nombre;
    private UsuarioResumenDTO owner;
    private List<UsuarioResumenDTO> miembros;
    private Integer capacidad;
    private Integer totalIntegrantes;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
