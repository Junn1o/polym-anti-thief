package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;

public class PolymantithiefClient implements ClientModInitializer {
	public static final String MOD_ID = "polym-anti-thief";

	@Override
	public void onInitializeClient() {
		ModNetworking.init();
	}

}