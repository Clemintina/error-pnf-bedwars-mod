package me.errorpnf.bedwarsmod.autoupdate;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.errorpnf.bedwarsmod.BedwarsMod;
import me.errorpnf.bedwarsmod.utils.formatting.FormatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class GithubAutoupdater {
    public static final String pfx = BedwarsMod.prefix;

    private static final String GITHUB_API_URL = "https://api.github.com/repos/error-PNF/BedwarsMod/releases/latest";
    private static String latestVersion;
    private static String downloadUrl;
    private static boolean hasPromptedUpdate = false;

    public static boolean isOutdated = false;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new GithubAutoupdater());
        checkForUpdates();
    }

    private static String changelog = "";

    public static void checkForUpdates() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(GITHUB_API_URL)
                .build();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    future.completeExceptionally(new IOException("Unexpected code " + response));
                    UChat.chat("&cAn unexpected error occurred during the API request. This is likely a rate limit issue.");
                    return;
                }

                try {
                    String jsonResponse = getJsonFromUrl(GITHUB_API_URL);
                    JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();
                    latestVersion = json.get("tag_name").getAsString().replace("v", "");
                    downloadUrl = json.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
                    changelog = json.get("body").getAsString(); // Extract changelog
                    changelog = changelog.replaceAll("\r\n", "\n§7");

                    String currentVersion = BedwarsMod.VERSION;

                    System.out.println("Current Mod Version: " + currentVersion);
                    System.out.println("Latest version from GitHub: " + latestVersion);
                    System.out.println("Download URL: " + downloadUrl);
                    System.out.println("Changelog: " + changelog); // Print changelog

                    if (!latestVersion.equals(currentVersion)) {
                        isOutdated = true;
                        System.out.println("An update is available. Current version: " + currentVersion);
                        hasPromptedUpdate = false;
                    }
                    future.complete(json);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(EntityJoinWorldEvent event) {
        if (!isOutdated || hasPromptedUpdate || event.entity != Minecraft.getMinecraft().thePlayer) return;

        hasPromptedUpdate = true;
        String currentVersion = BedwarsMod.VERSION;

        IChatComponent message = new ChatComponentText(pfx + FormatUtils.format("&7A new version of &bBedwars Mod &7is available! "));
        IChatComponent downloadLink = new ChatComponentText(pfx + FormatUtils.format("&7Click &b&nhere&r&7 to download."));
        downloadLink.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUrl));

        // Include changelog in the message
        IChatComponent changelogMessage = new ChatComponentText(pfx + FormatUtils.format("&aChangelog:\n&7" + changelog));

        Minecraft.getMinecraft().thePlayer.addChatMessage(message);
        Minecraft.getMinecraft().thePlayer.addChatMessage(downloadLink);
        Minecraft.getMinecraft().thePlayer.addChatMessage(changelogMessage); // Send changelog to player
        UChat.chat(pfx + "&cBedwars Mod v" + currentVersion + " &b-> " + "&aBedwars Mod v" + latestVersion);
    }

    private static String getJsonFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}