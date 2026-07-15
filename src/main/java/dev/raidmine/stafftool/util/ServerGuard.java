package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ServerGuard {
    private ServerGuard() {
    }

    public static boolean isAllowed(MinecraftClient client) {
        if (!RaidMineStaffMod.config().restrictToRaidMine) {
            return true;
        }
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        return RaidMineStaffMod.config().allowedAddressFragments.stream()
                .filter(fragment -> fragment != null && !fragment.isBlank())
                .map(fragment -> fragment.toLowerCase(Locale.ROOT))
                .anyMatch(context::contains);
    }

    public static String currentAddress(MinecraftClient client) {
        ServerInfo server = client.getCurrentServerEntry();
        return server == null || server.address == null ? "одиночная игра" : server.address;
    }

    public static boolean isActivityCounted(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null) return false;
        if (!isAllowed(client)) return false;
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        for (String keyword : RaidMineStaffMod.config().pausedServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return false;
        }
        for (String keyword : RaidMineStaffMod.config().activeServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return true;
        }
        return !context.contains("hub") && !context.contains("lobby") && !context.contains("limbo");
    }

    public static String detectMode(MinecraftClient client) {
        if (client == null || client.player == null || client.getNetworkHandler() == null) return "—";
        String context = collectContext(client).toLowerCase(Locale.ROOT);
        for (String keyword : RaidMineStaffMod.config().pausedServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return keyword.toUpperCase(Locale.ROOT);
        }
        for (String keyword : RaidMineStaffMod.config().activeServerKeywords) {
            if (!keyword.isBlank() && context.contains(keyword)) return keyword.toUpperCase(Locale.ROOT);
        }
        return "RAIDMINE";
    }

    private static String collectContext(MinecraftClient client) {
        List<String> parts = new ArrayList<>();
        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            add(parts, server.name);
            add(parts, server.address);
        }
        add(parts, invokeToString(client.getNetworkHandler(), "getBrand"));
        add(parts, invokeToString(client.getNetworkHandler(), "getServerBrand"));
        add(parts, invokeToString(client.getNetworkHandler(), "getPlayerListHeader"));
        add(parts, invokeToString(client.getNetworkHandler(), "getPlayerListFooter"));
        add(parts, invokeToString(client.player, "getDisplayName"));
        add(parts, invokeToString(client.player, "getName"));
        add(parts, invokeScoreboardSidebar(client));
        return String.join(" | ", parts);
    }

    private static String invokeScoreboardSidebar(MinecraftClient client) {
        try {
            Object scoreboard = client.player.getScoreboard();
            if (scoreboard == null) return "";
            for (Method method : scoreboard.getClass().getMethods()) {
                if (!method.getName().toLowerCase(Locale.ROOT).contains("sidebar")) continue;
                if (method.getParameterCount() != 0) continue;
                Object value = method.invoke(scoreboard);
                if (value != null) return value.toString();
            }
            return scoreboard.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String invokeToString(Object target, String methodName) {
        if (target == null) return "";
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank()) parts.add(value);
    }
}
