package io.github.devskycore.faunareborn.config.core;

public record ActivationConfig(
        double chance,
        boolean onlyNaturalChickens,
        boolean ignoreNamed
) {
}

