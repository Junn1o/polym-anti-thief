package com.junnio.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.util.*;


public class PlayerDataCache {
    private static final Map<String, Boolean> playerCache = new HashMap<>();

    public static boolean hasPlayerData(String name, MinecraftServer server) {
        if (name == null || name.isBlank()) return false;

        String lowerName = name.toLowerCase();

        // Check cache
        if (playerCache.containsKey(lowerName)) {
            return playerCache.get(lowerName);
        }

        // Lookup
        Optional<GameProfile> profileOpt = server.getUserCache()
                .findByName(lowerName)
                .filter(profile -> profile.getName().equalsIgnoreCase(lowerName));

        boolean exists = false;
        if (profileOpt.isPresent()) {
            UUID uuid = profileOpt.get().getId();
            File playerdataFile = server.getSavePath(WorldSavePath.PLAYERDATA)
                    .resolve(uuid.toString() + ".dat").toFile();
            exists = playerdataFile.exists();
        }

        // Cache result permanently
        playerCache.put(lowerName, exists);
        return exists;
    }
}

