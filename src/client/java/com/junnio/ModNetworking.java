package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger("polymantithief");

    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(ShulkerLogPayload.ID, ShulkerLogPayload.PACKET_CODEC);
    }

    public static void registerS2CPackets() {

    }

    public static void sendShulkerLogPacket(String playerName, String shulkerName, String position, String dimension) {
        ShulkerLogPayload payload = new ShulkerLogPayload(playerName, shulkerName, position, dimension);
        ClientPlayNetworking.send(payload);
    }
}

