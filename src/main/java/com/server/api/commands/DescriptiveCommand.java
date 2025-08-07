package com.server.api.commands;

import net.minestom.server.command.builder.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DescriptiveCommand extends Command {

    private String description = "";

    public DescriptiveCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);
    }

    public DescriptiveCommand(@NotNull String name) {
        super(name);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
