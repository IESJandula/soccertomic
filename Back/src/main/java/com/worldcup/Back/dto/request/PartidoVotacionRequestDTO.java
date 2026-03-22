package com.worldcup.Back.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoVotacionRequestDTO {

    @NotNull
    @Min(0)
    private Integer golesEquipoAPropuesto;

    @NotNull
    @Min(0)
    private Integer golesEquipoBPropuesto;

    @NotNull
    private String intensidadPartido;

    @NotNull
    private Boolean partidoFueParejo;

    private List<Long> jugadoresDiferenciales;

    @Valid
    private List<PartidoCompaneroValoracionRequestDTO> valoracionesCompaneros;
}
