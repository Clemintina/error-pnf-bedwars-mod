package me.errorpnf.bedwarsmod.utils;

import cc.polyfrost.oneconfig.events.event.LocrawEvent;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import me.errorpnf.bedwarsmod.mixin.MixinGuiPlayerTabOverlay;
import net.minecraft.client.Minecraft;

public class HypixelLocraw {
    public static String gamemode;
    public static String serverID;
    public static String mapName;
    public static String rawGameType;

    public static String lastBedwarsGameServerID = "";

    public static boolean hasUpdatedLastGameServer = false;

    @Subscribe
    public void locrawEvent(LocrawEvent event) {
        gamemode = event.info.getGameMode();
        serverID = event.info.getServerId();
        mapName = event.info.getMapName();
        rawGameType = event.info.getRawGameType();

        if (rawGameType.contains("BEDWARS") && gamemode.contains("BEDWARS")) {
            lastBedwarsGameServerID = serverID;
            isInBedwarsGameLocraw = true;
        } else {
            isInBedwarsGameLocraw = false;
        }

        isInBedwarsLobby = rawGameType.contains("BEDWARS") && serverID.contains("lobby");

        hasUpdatedLastGameServer = true;
    }

    public static boolean getIsInBedwarsGame() {
        return isInBedwarsGame;
    }

    private static boolean isInBedwarsGame = false;

    public static boolean getIsInBedwarsGameLocraw() {
        return isInBedwarsGameLocraw;
    }

    private static boolean isInBedwarsGameLocraw = false;

    public static boolean getIsInBedwarsLobby() {
        return isInBedwarsLobby;
    }

    private static boolean isInBedwarsLobby = false;

    @Subscribe
    public void onTick(TickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.ingameGUI == null) {
            return;
        }

        Object tabList = minecraft.ingameGUI.getTabList();
        if (!(tabList instanceof MixinGuiPlayerTabOverlay)) {
            return;
        }

        MixinGuiPlayerTabOverlay tabData = (MixinGuiPlayerTabOverlay) tabList;
        if (tabData.getTabFooter() == null) {
            return;
        }

        String footerData = tabData.getTabFooter().getUnformattedText();
        if (footerData == null) {
            return;
        }

        isInBedwarsGame = footerData.contains("Kills") && footerData.contains("Beds Broken");
    }
}
