package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioVotacionResumenDTO {
    private Integer vecesDiferencial;
    private Integer valoracionesPositivas;
    private Integer valoracionesNegativas;
}
