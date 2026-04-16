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

    // Resultado principal para compatibilidad con consumidores existentes.
    private Integer golesEquipoA;
    private Integer golesEquipoB;
    private String ganador;

    // Resultado base definido por organizacion (si existe).
    private Integer golesBaseEquipoA;
    private Integer golesBaseEquipoB;
    private String ganadorBase;

    // Resultado consensuado por votacion (si existe).
    private Integer golesConsensuadoEquipoA;
    private Integer golesConsensuadoEquipoB;
    private String ganadorConsensuado;
    private Boolean mostrarAmbosResultados;

    private String resultadoParaUsuario;
    private Boolean usuarioEnEquipoA;
    private String tipo;
    private String intensidadPartido;
    private Double porcentajeBalanceo;
    private Integer vecesDiferencial;
}