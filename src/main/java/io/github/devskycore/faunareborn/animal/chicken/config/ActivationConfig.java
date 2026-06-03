package io.github.devskycore.faunareborn.animal.chicken.config;

public record ActivationConfig(
        double chance,
        boolean onlyNaturalChickens,
        boolean ignoreNamed,
        int adultWithoutBabyGraceTicks
) {
}


