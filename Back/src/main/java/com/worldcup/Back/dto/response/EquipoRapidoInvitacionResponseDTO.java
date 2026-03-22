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
public class EquipoRapidoInvitacionResponseDTO {
    private Long id;
    private Long equipoRapidoId;
    private String equipoRapidoNombre;
    private UsuarioResumenDTO emisor;
    private UsuarioResumenDTO destinatario;
    private String estado;
    private String mensaje;
    private LocalDateTime creadaEn;
    private LocalDateTime respondidaEn;
}
