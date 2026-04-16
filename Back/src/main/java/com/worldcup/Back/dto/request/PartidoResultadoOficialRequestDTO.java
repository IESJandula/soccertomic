package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoResultadoOficialRequestDTO {

    @NotNull(message = "Los goles del equipo A son obligatorios")
    @Min(value = 0, message = "Los goles del equipo A no pueden ser negativos")
    private Integer golesEquipoA;

    @NotNull(message = "Los goles del equipo B son obligatorios")
    @Min(value = 0, message = "Los goles del equipo B no pueden ser negativos")
    private Integer golesEquipoB;
}
