package com.server.api;

import com.server.api.config.ServerConfig;
import com.server.api.maps.AbstractMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minestom.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractServer {

    public static final Path ROOT = Path.of("");
    public static final Path DATA = ROOT.resolve("data");
    public static final Path PLAYER_DATA = DATA.resolve("players");
    public static final Path SERVER_DATA = DATA.resolve("server");
    public static final Path WORLDS = ROOT.resolve("maps");
    public static final Path SCHEMATICS = ROOT.resolve("schematics");
    public static final Path IMAGES = ROOT.resolve("images");
    public static final Path TRIGGERS = ROOT.resolve("triggers");
    public static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractServer.class);

    protected final Object2ObjectOpenHashMap<String, AbstractMap> maps = new Object2ObjectOpenHashMap<>();

    protected static ServerConfig config;

    static {
        try {
            LOGGER.info("Initializing Files and Folders...");
            if (Files.notExists(WORLDS)) Files.createDirectories(WORLDS);
            if (Files.notExists(SCHEMATICS)) {
                Files.createDirectories(SCHEMATICS);
                try (var is = AbstractServer.class.getResourceAsStream("/default.schem")) {
                    Files.copy(is, SCHEMATICS.resolve("default.schem"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    MinecraftServer.getExceptionManager().handleException(e);
                }
            }
            if (Files.notExists(IMAGES)) {
                Files.createDirectories(IMAGES);
                try (var is = AbstractServer.class.getResourceAsStream("/image.png")) {
                    Files.copy(is, IMAGES.resolve("image.png"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    MinecraftServer.getExceptionManager().handleException(e);
                }
            }

            if (Files.notExists(DATA)) Files.createDirectories(DATA);
            if (Files.notExists(PLAYER_DATA)) Files.createDirectories(PLAYER_DATA);
            if (Files.notExists(SERVER_DATA)) Files.createDirectories(SERVER_DATA);

            if (Files.notExists(ROOT.resolve("messages.properties"))) {
                try (var is = AbstractServer.class.getResourceAsStream("/messages.properties")) {
                    Files.copy(is, ROOT.resolve("messages.properties"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    MinecraftServer.getExceptionManager().handleException(e);
                }
            }

            if (Files.notExists(ROOT.resolve("server.conf"))) {
                try (var is = AbstractServer.class.getResourceAsStream("/server.conf")) {
                    Files.copy(is, ROOT.resolve("server.conf"), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            config = ServerConfig.load(ROOT.resolve("server.conf"));

            LOGGER.info("   Loading server properties data...");
            System.setProperty("minestom.chunk-view-distance", String.valueOf(config.viewDistance().player()));
            System.setProperty("minestom.entity-view-distance", String.valueOf(config.viewDistance().entity()));

            LOGGER.info("Files and Folders Initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public ServerConfig config() {
        return config;
    }

    public abstract void init() throws Exception;

    public int port() {
        return config().general().port();
    }

    public String ipAddr() {
        return config().general().ip();
    }

    public Object2ObjectOpenHashMap<String, AbstractMap> maps() {
        return maps;
    }

    public AbstractMap getMap(String map) {
        return maps.get(map);
    }

    public void addMap(String id, AbstractMap map) {
        maps.put(id, map);
        MinecraftServer.getInstanceManager().registerInstance(map);
    }

    public boolean hasMap(String id) {
        return maps.containsKey(id);
    }

    public boolean removeMap(String id) {
        MinecraftServer.getInstanceManager().unregisterInstance(maps.remove(id));
        return maps.containsKey(id);
    }
}
