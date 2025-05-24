package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import com.junnio.storage.DatabaseManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Polymantithief implements ModInitializer {
	public static final String MOD_ID = "polym-anti-thief";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Initialize database only on server side
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			DatabaseManager.initDatabase();
		});

		// Register shutdown hook
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			DatabaseManager.shutdown();
		});

		// Register the server-side packet handler
		PayloadTypeRegistry.playC2S().register(ShulkerLogPayload.ID, ShulkerLogPayload.PACKET_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ShulkerLogPayload.ID, (payload, context) -> {
			context.server().execute(() -> {
				DatabaseManager.insertLog(
						payload.playerName(),
						payload.actionName(),
						payload.itemName(),
						payload.shulkerName(),
						payload.position(),
						payload.dimension(),
						payload.isContainer()
				);
			});
		});
	}
}