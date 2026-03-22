package com.worldcup.Back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartidoCompaneroValoradoEmbeddable {

    @Column(name = "jugador_id", nullable = false)
    private Long jugadorId;

    @Column(name = "puntuacion", nullable = false)
    private Integer puntuacion;
}
