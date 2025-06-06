package com.junnio.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ModNetworking {

    public static void init() {

    }

    public static void sendShulkerLogPacket(String playerName, String shulkerName, Float x,Float y, Float z, String dimension, boolean isContainer, String actionName, String itemName) {
        ShulkerLogPayload payload = new ShulkerLogPayload(playerName, shulkerName, x, y, z, dimension, isContainer, actionName, itemName);
        ClientPlayNetworking.send(payload);
    }
}

