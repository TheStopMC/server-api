package com.server.api.maps;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AbstractMap extends InstanceContainer {
    public AbstractMap(@NotNull UUID uuid, @NotNull RegistryKey<DimensionType> dimensionType) {
        super(uuid, dimensionType);
    }

    public AbstractMap(@NotNull UUID uuid, @NotNull RegistryKey<DimensionType> dimensionType, @NotNull Key dimensionName) {
        super(uuid, dimensionType, dimensionName);
    }

    public AbstractMap(@NotNull UUID uuid, @NotNull RegistryKey<DimensionType> dimensionType, @Nullable IChunkLoader loader) {
        super(uuid, dimensionType, loader);
    }

    public AbstractMap(@NotNull UUID uuid, @NotNull RegistryKey<DimensionType> dimensionType, @Nullable IChunkLoader loader, @NotNull Key dimensionName) {
        super(uuid, dimensionType, loader, dimensionName);
    }

    public AbstractMap(@NotNull DynamicRegistry<DimensionType> dimensionTypeRegistry, @NotNull UUID uuid, @NotNull RegistryKey<DimensionType> dimensionType, @Nullable IChunkLoader loader, @NotNull Key dimensionName) {
        super(dimensionTypeRegistry, uuid, dimensionType, loader, dimensionName);
    }

    public abstract AbstractMapData data();
}
