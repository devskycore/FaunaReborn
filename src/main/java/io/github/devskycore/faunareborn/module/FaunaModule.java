package io.github.devskycore.faunareborn.module;

public interface FaunaModule {

    String id();

    boolean isEnabledByConfig();

    void enable();

    void disable();
}
