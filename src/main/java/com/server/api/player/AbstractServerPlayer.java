package com.server.api.player;

import com.server.api.AbstractServer;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.util.Tristate;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerFlag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.collision.Aerodynamics;
import net.minestom.server.collision.PhysicsResult;
import net.minestom.server.collision.PhysicsUtils;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.entity.EntityVelocityEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.potion.TimedPotion;
import net.minestom.server.utils.chunk.ChunkUtils;
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
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public abstract class AbstractServerPlayer extends Player implements CombatPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServerPlayer.class);
    private static final Codec<List<ItemStack>> ITEMSTACK_CODEC = ItemStack.CODEC.list();
    protected static AbstractServer server;

    protected CompoundBinaryTag playerTag;

    protected final Path playersFile;
    protected final LuckPerms luckPerms;
    protected final PlayerAdapter<Player> playerAdapter;

    protected final AtomicLong balance;
    protected final Long firstJoin;
    protected Long lastJoin;
    protected Long lastQuit;
    protected Integer quits;
    protected Long totalPlayTime;
    protected final Inventory enderChestInventory = new Inventory(InventoryType.CHEST_3_ROW, Component.text("Ender Chest"));

    private boolean velocityUpdate = false;
    private PhysicsResult previousPhysicsResult = null;

    protected AbstractServerPlayer(LuckPerms luckPerms, @NotNull PlayerConnection playerConnection, @NotNull GameProfile gameProfile) {
        super(playerConnection, gameProfile);
        this.playersFile = AbstractServer.PLAYER_DATA.resolve(gameProfile.uuid() + ".dat");
        this.luckPerms = luckPerms;
        this.playerAdapter = this.luckPerms.getPlayerAdapter(Player.class);
        getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(1.0);

        if (Files.exists(playersFile)) {
            playerTag = (CompoundBinaryTag) read(playersFile).asBinaryTag();
            balance = new AtomicLong(playerTag.getLong("Balance", 200L));
            firstJoin = playerTag.getLong("FirstJoin", System.currentTimeMillis());
            lastJoin = System.currentTimeMillis();
            lastQuit = playerTag.getLong("LastQuit", -1L);
            quits = playerTag.getInt("Quits", 0);
            totalPlayTime = playerTag.getLong("TotalPlayTime", 0L);
            setGameMode(GameMode.valueOf(playerTag.getString("GameMode", "ADVENTURE")));
            List<ItemStack> inventory = ITEMSTACK_CODEC.decode(Transcoder.NBT, playerTag.get("Items")).orElse(List.of(ItemStack.AIR));
            AtomicInteger slot = new AtomicInteger(0);
            inventory.forEach(i -> getInventory().setItemStack(slot.getAndIncrement(), i));
            List<ItemStack> enderChestInventoryList = ITEMSTACK_CODEC.decode(Transcoder.NBT, playerTag.get("EnderItems")).orElse(List.of(ItemStack.AIR));
            slot.set(0);
            enderChestInventoryList.forEach(i -> enderChestInventory.setItemStack(slot.getAndIncrement(), i));
        } else {
            balance = new AtomicLong(200L);
            firstJoin = System.currentTimeMillis();
            lastJoin = System.currentTimeMillis();
            lastQuit = -1L;
            quits = 0;
            totalPlayTime = 0L;
        }
    }

    @Override
    public void remove(boolean permanent) {
        onQuit();
        CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Saving {}'s data...", getUuid());

                if (Files.notExists(playersFile)) {
                    try {
                        Files.createFile(playersFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                CompoundBinaryTag.Builder global = CompoundBinaryTag.builder();
                global.putString("Username", getUsername());
                global.putLong("Balance", balance.get());
                global.putLong("FirstJoin", firstJoin);
                global.putLong("LastJoin", lastJoin);
                global.putLong("LastQuit", System.currentTimeMillis());
                quits += 1;
                global.putInt("Quits", quits);

                updateTotalPlayTime();
                global.putLong("TotalPlayTime", totalPlayTime);

                global.put("Items", ITEMSTACK_CODEC.encode(Transcoder.NBT, Arrays.asList(getInventory().getItemStacks())).orElseThrow());
                global.put("EnderItems", ITEMSTACK_CODEC.encode(Transcoder.NBT, Arrays.asList(enderChestInventory.getItemStacks())).orElseThrow());

                global.putFloat("Health", getHealth());
                global.putString("GameMode", getGameMode().toString());
                global.putFloat("Food", getFood());
                global.putFloat("Saturation", getFoodSaturation());
                write(playersFile, global.build());
            } catch (Throwable e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }, AbstractServer.EXECUTOR);
        super.remove(permanent);
    }

    public abstract void onQuit();

    public abstract User getLuckPermsUser();

    public abstract CachedMetaData getLuckPermsMetaData();

    public abstract @NotNull CompletableFuture<DataMutateResult> addPermission(@NotNull String permission);

    public abstract @NotNull CompletableFuture<DataMutateResult> setPermission(@NotNull Node permission, boolean value);

    public abstract @NotNull CompletableFuture<DataMutateResult> removePermission(@NotNull String permissionName);

    public abstract boolean hasPermission(@NotNull String permissionName);

    public abstract @NotNull Tristate getPermission(@NotNull String permissionName);

    public abstract @NotNull Component getPrefix();

    public abstract AtomicLong balance();

    public abstract Long bal();

    public abstract AtomicLong balance(Long balance);

    public abstract AtomicLong deposit(Long amount);

    public abstract AtomicLong withdraw(Long amount);

    public abstract Long firstJoin();

    public abstract Long lastJoin();

    public abstract Long lastQuit();

    public abstract Long totalPlayTime();
    public abstract void addPlayTime(Long playTime);
    public abstract void updateTotalPlayTime();

    public abstract Integer quits();

    public abstract Pos regionSelectionOne();

    public abstract void regionSelectionOne(Pos regionSelectionOne);

    public abstract Pos regionSelectionTwo();

    public abstract void regionSelectionTwo(Pos regionSelectionTwo);

    public abstract Deque<AbsoluteBlockBatch> worldEditUndoStack();

    public abstract void vanished(boolean isVanished);

    public abstract boolean vanished();

    public abstract Inventory getEnderChestInventory();

    public abstract CompoundBinaryTag getPlayerTag();


    private void write(Path playerFile, BinaryTag value) {
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

    @Override
    public void setVelocity(@NotNull Vec velocity) {
        EntityVelocityEvent entityVelocityEvent = new EntityVelocityEvent(this, velocity);
        EventDispatcher.callCancellable(entityVelocityEvent, () -> {
            this.velocity = entityVelocityEvent.getVelocity();
            velocityUpdate = true;
        });
    }

    @Override
    public void setVelocityNoUpdate(Function<Vec, Vec> function) {
        velocity = function.apply(velocity);
    }

    @Override
    public void sendImmediateVelocityUpdate() {
        if (velocityUpdate) {
            velocityUpdate = false;
            sendPacketToViewersAndSelf(getVelocityPacket());
        }
    }

    public boolean isOnGroundAfterTicks(int ticks) {
        if (vehicle != null) return false;

        final double tps = ServerFlag.SERVER_TICKS_PER_SECOND;
        Vec velocity = this.velocity.div(tps);
        Pos position = this.position;

        // Slow falling effect
        Aerodynamics aerodynamics = getAerodynamics();
        if (velocity.y() < 0 && hasEffect(PotionEffect.SLOW_FALLING))
            aerodynamics = aerodynamics.withGravity(0.01);

        // Do movementTick() calculations for the given amount of ticks
        PhysicsResult prevPhysicsResult = previousPhysicsResult;
        for (int i = 0; i < ticks; i++) {
            PhysicsResult physicsResult = PhysicsUtils.simulateMovement(position, velocity, boundingBox,
                    instance.getWorldBorder(), instance, aerodynamics, hasNoGravity(), hasPhysics, onGround, isFlying(), prevPhysicsResult);
            prevPhysicsResult = physicsResult;

            if (physicsResult.isOnGround()) return true;

            velocity = physicsResult.newVelocity();
            position = physicsResult.newPosition();

            // Levitation effect
            TimedPotion levitation = getEffect(PotionEffect.LEVITATION);
            if (levitation != null) {
                velocity = velocity.withY(
                        ((0.05 * (double) (levitation.potion().amplifier() + 1) - (velocity.y())) * 0.2)
                );
            }
        }

        return false;
    }

    @Override
    protected void movementTick() {
        this.gravityTickCount = onGround ? 0 : gravityTickCount + 1;
        if (vehicle != null) return;

        final double tps = ServerFlag.SERVER_TICKS_PER_SECOND;

        // Slow falling effect
        Aerodynamics aerodynamics = getAerodynamics();
        if (velocity.y() < 0 && hasEffect(PotionEffect.SLOW_FALLING))
            aerodynamics = aerodynamics.withGravity(0.01);

        PhysicsResult physicsResult = PhysicsUtils.simulateMovement(position, velocity.div(tps), boundingBox,
                instance.getWorldBorder(), instance, aerodynamics, hasNoGravity(), hasPhysics, onGround, isFlying(), previousPhysicsResult);
        this.previousPhysicsResult = physicsResult;

        Chunk finalChunk = ChunkUtils.retrieve(instance, currentChunk, physicsResult.newPosition());
        if (!ChunkUtils.isLoaded(finalChunk)) return;

        velocity = physicsResult.newVelocity().mul(tps);
        //onGround = physicsResult.isOnGround();

        // Levitation effect
        TimedPotion levitation = getEffect(PotionEffect.LEVITATION);
        if (levitation != null) {
            velocity = velocity.withY(
                    ((0.05 * (double)
                            (levitation.potion().amplifier() + 1)
                            - (velocity.y() / tps)) * 0.2) * tps
            );
        }

        //TODO
        //if (!PlayerUtils.isSocketClient(this)) {
        //	refreshPosition(physicsResult.newPosition(), true, true);
        //}
        sendImmediateVelocityUpdate();
    }
    public static void initServer(AbstractServer server) {
        AbstractServerPlayer.server = server;
    }
}
