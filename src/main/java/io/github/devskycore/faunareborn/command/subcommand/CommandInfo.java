package io.github.devskycore.faunareborn.command.subcommand;

public record CommandInfo(
        String name,
        String usage,
        String description,
        String permission,
        boolean administrative,
        java.util.List<String> aliases
) {
    public CommandInfo(String name, String usage, String description, String permission, boolean administrative) {
        this(name, usage, description, permission, administrative, java.util.List.of());
    }
}
