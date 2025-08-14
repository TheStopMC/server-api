package com.server.api.player;

import com.server.api.AbstractServer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractServerPlayer extends Player implements CombatPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerPlayer.class);
    protected static AbstractServer server;

    protected AbstractServerPlayer(LuckPerms luckPerms, @NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    public abstract User getLuckPermsUser();

    public abstract CachedMetaData getLuckPermsMetaData();

    public abstract @NotNull CompletableFuture<DataMutateResult> addPermission(@NotNull String permission);

    public abstract @NotNull CompletableFuture<DataMutateResult> setPermission(@NotNull Node permission, boolean value);

    public abstract @NotNull CompletableFuture<DataMutateResult> removePermission(@NotNull String permissionName);

    public abstract boolean hasPermission(@NotNull String permissionName);

    public abstract @NotNull Tristate getPermission(@NotNull String permissionName);

    public abstract @NotNull Component getPrefix();

    public static void initServer(AbstractServer server) {
        AbstractServerPlayer.server = server;
    }
}
