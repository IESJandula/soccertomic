package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.UsuarioEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class VisibleLevelService {

    private static final BigDecimal DEFAULT_MU = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_SIGMA = new BigDecimal("8.33");
    private static final BigDecimal SIGMA_MULTIPLIER = new BigDecimal("3.00");

    public BigDecimal calcularNivelVisible(UsuarioEntity usuario) {
        if (usuario == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal mu = usuario.getRatingMu() == null ? DEFAULT_MU : usuario.getRatingMu();
        BigDecimal sigma = usuario.getRatingSigma() == null ? DEFAULT_SIGMA : usuario.getRatingSigma();

        BigDecimal nivelVisible = mu.subtract(sigma.multiply(SIGMA_MULTIPLIER));
        if (nivelVisible.signum() < 0) {
            nivelVisible = BigDecimal.ZERO;
        }

        return nivelVisible.setScale(2, RoundingMode.HALF_UP);
    }

    public String calcularFiabilidadLabel(UsuarioEntity usuario) {
        if (usuario == null) {
            return "MEDIA";
        }

        int ausencias = usuario.getAusencias() == null ? 0 : usuario.getAusencias();
        int abandonos = usuario.getAbandonos() == null ? 0 : usuario.getAbandonos();
        int incidencias = ausencias + abandonos;

        if (incidencias <= 1) {
            return "ALTA";
        }
        if (incidencias <= 4) {
            return "MEDIA";
        }
        return "BAJA";
    }
}
