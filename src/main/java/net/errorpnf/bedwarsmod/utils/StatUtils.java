package net.errorpnf.bedwarsmod.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class StatUtils {
    private final JsonObject jsonObject;

    public StatUtils(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getStat(String statPath) {
        JsonElement statElement = jsonObject;
        String[] keys = statPath.split("\\.");

        for (String key : keys) {
            if (statElement != null && statElement.isJsonObject()) {
                statElement = statElement.getAsJsonObject().get(key);
            } else {
                return "Stat not found";
            }
        }

        return statElement != null && !statElement.isJsonNull() ? statElement.getAsString() : "Stat not found";
    }
}
