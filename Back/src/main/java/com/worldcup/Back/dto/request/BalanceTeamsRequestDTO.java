package com.worldcup.Back.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalanceTeamsRequestDTO {
    @NotEmpty(message = "Debes enviar al menos un jugador")
    private List<Long> playerIds;
}