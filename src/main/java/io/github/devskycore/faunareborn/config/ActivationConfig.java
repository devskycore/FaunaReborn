package io.github.devskycore.faunareborn.config;

public record ActivationConfig(
        double chance,
        boolean onlyNaturalChickens,
        boolean ignoreNamed
) {
}
