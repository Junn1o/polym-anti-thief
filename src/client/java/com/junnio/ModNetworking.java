package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ModNetworking {

    public static void init() {

    }

    public static void sendShulkerLogPacket(String playerName, String shulkerName, String position, String dimension, boolean isContainer) {
        ShulkerLogPayload payload = new ShulkerLogPayload(playerName, shulkerName, position, dimension, isContainer);
        ClientPlayNetworking.send(payload);
    }
}

