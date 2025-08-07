package com.server.api.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.server.api.AbstractServer;
import net.minestom.server.utils.url.URLUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UUIDFetcher {
    private static final String FROM_USERNAME_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/name/%s";

    /**
     * Gets a player's UUID from their username
     *
     * @param username The players username
     * @return The {@link UUID}
     */
    public static CompletableFuture<Optional<UUID>> fetchByName(String username) {
        return CompletableFuture.supplyAsync(() -> {
            // Thanks stackoverflow: https://stackoverflow.com/a/19399768/13247146
            if (username.length() > 16) return Optional.empty(); // Player names cannot be over 16 chars, no need to ping mojang
            try {
                return Optional.of(UUID.fromString(
                        retrieve(String.format(FROM_USERNAME_URL, username)).get("id")
                                .getAsString()
                                .replaceFirst(
                                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                                        "$1-$2-$3-$4-$5"
                                )
                ));
            } catch (IOException e) {
//                MinecraftServer.getExceptionManager().handleException(e);
                return Optional.empty();
            }
        }, AbstractServer.EXECUTOR);
    }

    /**
     * Gets the JsonObject from a URL, expects a mojang player URL so the errors might not make sense if it is not
     *
     * @param url The url to retrieve
     * @return The {@link JsonObject} of the result
     * @throws IOException with the text detailing the exception
     */
    private static JsonObject retrieve(@NotNull String url) throws IOException {
        // Retrieve from the rate-limited Mojang API
        final String response = URLUtils.getText(url);
        // If our response is "", that means the url did not get a proper object from the url
        // So the username or UUID was invalid, and therefore we return null
        if (response.isEmpty()) throw new IOException("The Mojang API is down");
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        if (jsonObject.has("errorMessage")) {
            throw new IOException(jsonObject.get("errorMessage").getAsString());
        }
        return jsonObject;
    }
}
