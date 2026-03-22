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
public class PartidoOrganizadorDTO {
    private UsuarioResumenDTO usuario;
    private String rol;
    private LocalDateTime asignadoEn;
}
