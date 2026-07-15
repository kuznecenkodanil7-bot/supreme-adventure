package dev.raidmine.stafftool.util;

import dev.raidmine.stafftool.RaidMineStaffMod;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;
import java.util.Map;

public final class AuthManager {
    private static volatile boolean authenticated;
    private static volatile String authenticatedUser = "";

    private AuthManager() {
    }

    public static boolean canUseMod() {
        MinecraftClient client = MinecraftClient.getInstance();
        String username = currentSessionName(client);
        return isAllowedUsername(username) && authenticated && username.equalsIgnoreCase(authenticatedUser);
    }

    public static boolean needsLogin(MinecraftClient client) {
        String username = currentSessionName(client);
        if (username.isBlank()) return false;
        if (!isAllowedUsername(username)) return true;
        return !(authenticated && username.equalsIgnoreCase(authenticatedUser));
    }

    public static boolean isAllowedUsername(String username) {
        if (username == null || username.isBlank()) return false;
        for (String allowed : RaidMineStaffMod.config().staffCredentials.keySet()) {
            if (allowed.equalsIgnoreCase(username)) return true;
        }
        return false;
    }

    public static boolean login(String password) {
        MinecraftClient client = MinecraftClient.getInstance();
        String username = currentSessionName(client);
        if (!isAllowedUsername(username)) {
            authenticated = false;
            authenticatedUser = "";
            return false;
        }
        for (Map.Entry<String, String> entry : RaidMineStaffMod.config().staffCredentials.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(username) && entry.getValue().equals(password)) {
                authenticated = true;
                authenticatedUser = entry.getKey();
                return true;
            }
        }
        return false;
    }

    public static void logout() {
        authenticated = false;
        authenticatedUser = "";
    }

    public static String currentSessionName(MinecraftClient client) {
        if (client == null) return "";
        try {
            Object session = client.getSession();
            if (session == null) return "";
            for (String methodName : new String[]{"getUsername", "getUsernameOrNull", "username", "name"}) {
                try {
                    Method method = session.getClass().getMethod(methodName);
                    Object result = method.invoke(session);
                    if (result != null && !result.toString().isBlank()) return result.toString();
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return session.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
