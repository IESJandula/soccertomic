package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoBalanceAnalisisResponseDTO {
    private Long partidoId;
    private Boolean balanceado;
    private Integer diferenciaNivel;
    private Integer jugadoresEquipoA;
    private Integer jugadoresEquipoB;
    private List<String> razonesDesbalance;
}
