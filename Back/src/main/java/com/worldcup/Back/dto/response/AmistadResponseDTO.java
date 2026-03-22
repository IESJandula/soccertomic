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
public class AmistadResponseDTO {
    private Long id;
    private UsuarioResumenDTO usuarioA;
    private UsuarioResumenDTO usuarioB;
    private String estado;
    private LocalDateTime creadaEn;
    private LocalDateTime aceptadaEn;
}
