package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EquipoRapidoRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String nombre;

    @Size(max = 6)
    private List<Long> miembroIds;

    @Min(2)
    @Max(7)
    private Integer capacidad;
}
