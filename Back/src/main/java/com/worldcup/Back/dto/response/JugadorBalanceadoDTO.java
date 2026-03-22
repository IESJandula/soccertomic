package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JugadorBalanceadoDTO {
    private Long id;
    private String nombre;
    private String posicionAsignada;
    private Integer nivelCalculado;
}