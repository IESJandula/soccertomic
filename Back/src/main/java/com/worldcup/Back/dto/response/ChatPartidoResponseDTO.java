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
public class ChatPartidoResponseDTO {
    private Long id;
    private Long partidoId;
    private UsuarioResumenDTO usuario;
    private String mensajePredefinido;
    private LocalDateTime creadoEn;
}
