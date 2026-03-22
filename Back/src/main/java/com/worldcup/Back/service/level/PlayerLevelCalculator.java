package com.worldcup.Back.service.level;

import com.worldcup.Back.entity.PlayerProfileEntity;

public interface PlayerLevelCalculator {
    int calculate(PlayerProfileEntity profile);
    String version();
}