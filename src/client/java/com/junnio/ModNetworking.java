package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger("polymantithief");

    public static void init() {

    }

    public static void sendShulkerLogPacket(String playerName, String shulkerName, String position, String dimension) {
        ShulkerLogPayload payload = new ShulkerLogPayload(playerName, shulkerName, position, dimension);
        ClientPlayNetworking.send(payload);
    }
}

