package com.server.api.config;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.DefaultObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMapperFactory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.nio.file.Path;
import java.util.List;

public class ServerConfig {
    @Setting("server")
    private GeneralSettingsConfig general = new GeneralSettingsConfig();

    @Setting("modules")
    private ModulesConfig modules = new ModulesConfig();

    @Setting("view-distance")
    private ViewDistanceConfig viewDistance = new ViewDistanceConfig();

    @Setting("world")
    private WorldConfig world = new WorldConfig();

    @Setting("resource-pack")
    private ResourcePackConfig resourcePack = new ResourcePackConfig();

    public ServerConfig() {}

    public GeneralSettingsConfig general() {
        return general;
    }

    public ModulesConfig modules() {
        return modules;
    }

    public ViewDistanceConfig viewDistance() {
        return viewDistance;
    }

    public WorldConfig world() {
        return world;
    }

    public ResourcePackConfig resourcePack() {
        return resourcePack;
    }

    @ConfigSerializable
    public static class GeneralSettingsConfig {
        @Setting("ip")
        private String ip;

        @Setting("port")
        private int port;

        @Setting("velocity-secret")
        private String velocitySecret;

        @Setting("online-mode")
        private boolean onlineMode;

        @Setting("motd")
        private String motd;

        @Setting("max-players")
        private int maxPlayers;

        @Setting("network-compression-threshold")
        private int networkCompressionThreshold;

        @Setting("branding")
        private String branding;

        public String ip() {
            return ip;
        }

        public int port() {
            return port;
        }

        public String velocitySecret() {
            return velocitySecret;
        }

        public boolean onlineMode() {
            return onlineMode;
        }

        public String motd() {
            return motd;
        }

        public int maxPlayers() {
            return maxPlayers;
        }

        public int networkCompressionThreshold() {
            return networkCompressionThreshold;
        }

        public String branding() {
            return branding;
        }
    }

    @ConfigSerializable
    public static class ModulesConfig {
        @Setting("enabled")
        private List<String> enabled;

        public List<String> enabled() {
            return enabled;
        }
    }

    @ConfigSerializable
    public static class ViewDistanceConfig {
        @Setting("player")
        private int player;

        @Setting("entity")
        private int entity;

        public int player() {
            return player;
        }

        public int entity() {
            return entity;
        }
    }

    @ConfigSerializable
    public static class WorldConfig {
        @Setting("default")
        private String defaultWorld;

        @Setting("schematic")
        private String schematic;

        public String defaultWorld() {
            return defaultWorld;
        }

        public String schematic() {
            return schematic;
        }
    }

    @ConfigSerializable
    public static class ResourcePackConfig {
        @Setting("url")
        private String url;

        @Setting("id")
        private String id;

        @Setting("prompt")
        private String prompt;

        @Setting("sha1")
        private String sha1;

        public String url() {
            return url;
        }

        public String id() {
            return id;
        }

        public String prompt() {
            return prompt;
        }

        public String sha1() {
            return sha1;
        }
    }



    public static ServerConfig load(Path path) throws Exception {
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                .setPath(path)
                .build();

        ConfigurationNode root = loader.load();

        ObjectMapperFactory factory = new DefaultObjectMapperFactory();
        ObjectMapper<ServerConfig> mapper = factory.getMapper(ServerConfig.class);
        return mapper.bindToNew().populate(root);
    }
}