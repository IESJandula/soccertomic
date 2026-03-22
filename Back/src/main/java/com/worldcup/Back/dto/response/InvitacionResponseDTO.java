package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InvitacionResponseDTO {
    private Long id;
    private Long partidoId;
    private UsuarioResumenDTO usuario;
    private String estado;
    private String mensaje;
    private BigDecimal precioTotalPista;
    private BigDecimal parteIndividual;
    private Long reservadoPorUsuarioId;
    private String reservadoPorNombre;
    private Boolean pagada;
    private LocalDateTime creadaEn;
    private LocalDateTime respondidaEn;
}
