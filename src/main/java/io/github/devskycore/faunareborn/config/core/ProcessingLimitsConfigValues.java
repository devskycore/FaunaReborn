package io.github.devskycore.faunareborn.config.core;

public record ProcessingLimitsConfigValues(
        int maxActiveHostileChickensPerChunk,
        int maxActiveHostileChickensPerWorld,
        int maxProcessedChickensPerTick
) {
}

