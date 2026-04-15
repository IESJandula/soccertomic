package com.worldcup.Back.entity.enums;

public enum TipoIncidenciaPartido {
    LESION,
    ABANDONO,
    AUSENCIA,
    CONDUCTA,
    OTRO;

    public static TipoIncidenciaPartido fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El tipo de incidencia es obligatorio");
        }

        String normalized = raw.trim().toUpperCase();
        try {
            return TipoIncidenciaPartido.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de incidencia no soportado: " + raw);
        }
    }
}
