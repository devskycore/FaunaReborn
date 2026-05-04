package io.github.devskycore.faunareborn.config.entity;

public enum EntityType {
    CHICKEN("chicken", "entities/chicken.yml"),
    COW("cow", "entities/cow.yml");

    private final String id;
    private final String resourcePath;

    EntityType(String id, String resourcePath) {
        this.id = id;
        this.resourcePath = resourcePath;
    }

    public String id() {
        return id;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
