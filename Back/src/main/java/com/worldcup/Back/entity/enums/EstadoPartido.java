package com.worldcup.Back.entity.enums;

import java.util.List;

public enum EstadoPartido {
    BORRADOR,
    ABIERTO,
    COMPLETO,
    EN_CURSO,
    FINALIZADO,
    @Deprecated
    CERRADO,
    CANCELADO,
    @Deprecated
    CREADO,
    CONFIRMADO,
    @Deprecated
    EN_JUEGO

    ;

    public EstadoPartido canonical() {
        return switch (this) {
            case CREADO -> BORRADOR;
            case EN_JUEGO -> EN_CURSO;
            case CERRADO -> FINALIZADO;
            default -> this;
        };
    }

    public boolean isFinalizado() {
        return this == FINALIZADO || this == CERRADO;
    }

    public static List<EstadoPartido> estadosFinalizadosCompatibles() {
        return List.of(FINALIZADO, CERRADO);
    }
}
