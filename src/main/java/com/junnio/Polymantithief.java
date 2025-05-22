package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Polymantithief implements ModInitializer {
	public static final String MOD_ID = "polym-anti-thief";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(ShulkerLogPayload.ID, ShulkerLogPayload.PACKET_CODEC);
		// Register the server-side packet handler
		ServerPlayNetworking.registerGlobalReceiver(ShulkerLogPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				LOGGER.info("{} "+"borrowed"+" someone else's Shulker named: {} at {} in {} from a container",
						payload.playerName(),
						payload.shulkerName(),
						payload.position(),
						payload.dimension()
				);

			});
		});
		LOGGER.info("Polymantithief initialized!");
		LOGGER.info("Hello Fabric world!");
	}
}