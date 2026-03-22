package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoVotacionResponseDTO {
    private Long id;
    private Long partidoId;
    private UsuarioResumenDTO votante;
    private Integer golesEquipoAPropuesto;
    private Integer golesEquipoBPropuesto;
    private String intensidadPartido;
    private Boolean partidoFueParejo;
    private List<Long> jugadoresDiferenciales;
    private List<PartidoCompaneroAsignadoDTO> companerosAsignados;
    private List<PartidoCompaneroValoracionDTO> valoracionesCompaneros;
    private LocalDateTime actualizadaEn;
}
