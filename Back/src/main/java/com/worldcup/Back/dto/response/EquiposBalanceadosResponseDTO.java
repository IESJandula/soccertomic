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
public class EquiposBalanceadosResponseDTO {
    private Long partidoId;
    private Integer nivelTotalEquipoA;
    private Integer nivelTotalEquipoB;
    private Integer diferenciaNivelAntes;
    private Integer diferenciaNivelDespues;
    private Boolean balanceadoAntes;
    private Boolean balanceadoDespues;
    private List<String> razonesDesbalance;
    private List<String> cambiosAplicados;
    private List<JugadorBalanceadoDTO> equipoA;
    private List<JugadorBalanceadoDTO> equipoB;
}