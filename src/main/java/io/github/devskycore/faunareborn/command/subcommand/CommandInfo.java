package io.github.devskycore.faunareborn.command.subcommand;

public record CommandInfo(
        String name,
        String usage,
        String description,
        String permission,
        boolean administrative
) {
}
