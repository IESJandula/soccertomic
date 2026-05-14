package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.PlayerProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class DefaultPlayerLevelCalculator implements PlayerLevelCalculator {

    @Override
    public int calculate(PlayerProfileEntity profile) {
        // El nivel visible se calcula desde TrueSkill a nivel de usuario; aquí dejamos un valor neutro.
        return 5;
    }

    @Override
    public String version() {
        return "v1";
    }
}