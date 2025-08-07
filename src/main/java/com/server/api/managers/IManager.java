package com.server.api.managers;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.UUID;

public interface IManager {


    Gson gson = new GsonBuilder()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(BaltopEntry.class, new BaltopEntryTypeAdapter())
            .serializeNulls()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
            .setPrettyPrinting()
            .create();

    void save();


    record BaltopEntry(UUID playerID, String username, Long balance) { }

    class BaltopEntryTypeAdapter implements JsonSerializer<BaltopEntry>, JsonDeserializer<BaltopEntry> {
        @Override
        public BaltopEntry deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject global = jsonElement.getAsJsonObject();

            return new BaltopEntry(UUID.fromString(global.get("playerID").getAsString()),
                    global.get("username").getAsString(),
                    global.get("balance").getAsLong());
        }

        @Override
        public JsonElement serialize(BaltopEntry entry, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject global = new JsonObject();

            global.add("playerID", new JsonPrimitive(entry.playerID().toString()));
            global.add("username", new JsonPrimitive(entry.username()));
            global.add("balance", new JsonPrimitive(entry.balance));

            return global;
        }
    }

}
