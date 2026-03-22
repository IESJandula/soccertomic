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
public class UsuarioResumenDTO {
    private Long id;
    private String nombre;
    private String email;
    private String nivel;
    private Integer reputacionPositiva;
    private List<String> rasgos;
    private String skillTier;
    private String playTendency;
}
