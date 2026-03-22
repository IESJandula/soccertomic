package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.PlayerProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class DefaultPlayerLevelCalculator implements PlayerLevelCalculator {

    @Override
    public int calculate(PlayerProfileEntity profile) {
        // Convertir globalRating (0-5) a escala de nivel (1-10)
        if (profile.getGlobalRating() != null) {
            return Math.round(profile.getGlobalRating() * 2);
        }
        return 5; // Default if no rating
    }

    @Override
    public String version() {
        return "v1";
    }
}