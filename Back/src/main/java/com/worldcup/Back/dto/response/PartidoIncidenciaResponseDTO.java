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
public class PartidoIncidenciaResponseDTO {
    private Long id;
    private Long partidoId;
    private Long usuarioAfectadoId;
    private String usuarioAfectadoNombre;
    private Long reportadoPorId;
    private String reportadoPorNombre;
    private String tipoIncidencia;
    private Integer severidad;
    private Integer minuto;
    private String comentario;
    private Boolean validadaPorOrganizador;
    private LocalDateTime creadaEn;
}
