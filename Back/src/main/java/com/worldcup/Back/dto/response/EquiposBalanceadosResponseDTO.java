package com.worldcup.Back.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple balance result: only exposes final levels and state.
 * Keeps internals opaque (no explanations, no intermediate data).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EquiposBalanceadosResponseDTO {
    private Long partidoId;
    private Integer nivelTotalEquipoA;
    private Integer nivelTotalEquipoB;
    private Boolean balanceadoDespues;
    
    // Deprecated: kept for backward compatibility but no longer used
    private Integer diferenciaNivelAntes;
    private Integer diferenciaNivelDespues;
    private Boolean balanceadoAntes;
    private String resumenOrganizador;
    private String motivoPrincipal;
}