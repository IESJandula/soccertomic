package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.Max;
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
public class PartidoCompaneroValoracionRequestDTO {

    @NotNull
    private Long jugadorId;

    @NotNull
    @Min(-1)
    @Max(1)
    private Integer puntuacion;
}
