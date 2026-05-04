package io.github.devskycore.faunareborn.animal.chicken.config;

public record ProcessingLimitsConfigValues(
        int maxActiveHostileChickensPerChunk,
        int maxActiveHostileChickensPerWorld,
        int maxProcessedChickensPerTick
) {
}


