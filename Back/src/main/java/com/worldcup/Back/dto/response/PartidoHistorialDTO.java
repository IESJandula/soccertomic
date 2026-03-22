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
public class PartidoHistorialDTO {
    private Long id;
    private LocalDateTime fecha;
    private String lugar;
    private Integer golesEquipoA;
    private Integer golesEquipoB;
    private String ganador;
    private String resultadoParaUsuario;
    private Boolean usuarioEnEquipoA;
    private String tipo;
    private String intensidadPartido;
    private Double porcentajeBalanceo;
    private Integer vecesDiferencial;
}