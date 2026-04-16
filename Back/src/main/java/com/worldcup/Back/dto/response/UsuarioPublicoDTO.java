package com.worldcup.Back.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioPublicoDTO {
    private Long id;
    private String nombre;
    private Integer puntos;
    private List<String> rasgos;
}
