package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoIncidenciaRequestDTO {

    private Long usuarioId;

    @NotBlank
    private String tipoIncidencia;

    @Min(1)
    @Max(3)
    private Integer severidad = 2;

    @Min(0)
    private Integer minuto;

    private String comentario;
}
