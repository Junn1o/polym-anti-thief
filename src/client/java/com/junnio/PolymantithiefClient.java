package com.junnio;

import com.junnio.net.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import java.util.concurrent.CompletableFuture;

import static com.junnio.Polymantithief.HANDSHAKE_CHANNEL;

public class PolymantithiefClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModNetworking.init();
		ClientLoginNetworking.registerGlobalReceiver(HANDSHAKE_CHANNEL, (client, handler, buf, listenerAdder) -> {
			PacketByteBuf response = PacketByteBufs.create();
			response.writeBoolean(true);
			return CompletableFuture.completedFuture(response);
		});
	}
}