package com.server.api.player;

import com.server.api.AbstractServer;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.kyori.adventure.nbt.BinaryTag;
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
import net.minestom.server.utils.nbt.BinaryTagReader;
import net.minestom.server.utils.nbt.BinaryTagWriter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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

    public void write(Path playerFile, BinaryTag value) {
        Path dir = playerFile.getParent();
        String baseName = playerFile.getFileName().toString().replace(".dat", "");

        Path newFile = dir.resolve(baseName + ".dat_new");
        Path oldFile = dir.resolve(baseName + ".dat_old");

        try {
            // Step 1: Serialize NBT to a byte array
            ByteArrayOutputStream nbtByteArrayStream = new ByteArrayOutputStream();
            try (DataOutputStream dataOut = new DataOutputStream(nbtByteArrayStream)) {
                BinaryTagWriter nbtWriter = new BinaryTagWriter(dataOut);
                nbtWriter.writeNameless(value);
            }

            byte[] nbtData = nbtByteArrayStream.toByteArray();

            // Step 2: Compress the data using LZ4
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4Compressor compressor = factory.fastCompressor();

            int maxCompressedLength = compressor.maxCompressedLength(nbtData.length);
            byte[] compressed = new byte[maxCompressedLength];
            int compressedLength = compressor.compress(nbtData, 0, nbtData.length, compressed, 0, maxCompressedLength);

            // Step 3: Write compressed data to .dat_new
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(newFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.writeInt(nbtData.length); // original uncompressed length (needed for decompression)
                out.write(compressed, 0, compressedLength);
            }

            // Step 4: Backup and replace files
            if (Files.exists(playerFile)) {
                Files.move(playerFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(newFile, playerFile, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Failed to safely write player data for: " + playerFile.getFileName(), e);
        }
    }

    public static BinaryTag read(Path playersFile) {
        try (DataInputStream in = new DataInputStream(Files.newInputStream(playersFile, StandardOpenOption.READ))) {

            // Step 1: Read the uncompressed length
            int originalLength = in.readInt();

            // Step 2: Read the remaining compressed data
            byte[] compressed = in.readAllBytes();

            // Step 3: Decompress using LZ4
            LZ4Factory factory = LZ4Factory.fastestInstance();
            LZ4FastDecompressor decompressor = factory.fastDecompressor();

            byte[] decompressed = new byte[originalLength];
            decompressor.decompress(compressed, 0, decompressed, 0, originalLength);

            // Step 4: Use BinaryTagReader on decompressed data
            try (DataInputStream decompressedIn = new DataInputStream(new ByteArrayInputStream(decompressed))) {
                BinaryTagReader nbtReader = new BinaryTagReader(decompressedIn);
                return nbtReader.readNameless();
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read or decompress player data: " + playersFile.getFileName(), e);
        }
    }
}
