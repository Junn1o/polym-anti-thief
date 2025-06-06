package com.junnio;

import com.junnio.net.ShulkerLogPayload;
import com.junnio.storage.DatabaseManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Polymantithief implements ModInitializer {
	public static final String MOD_ID = "polym-anti-thief";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier HANDSHAKE_CHANNEL = Identifier.of(MOD_ID, "handshake");
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
						payload.x(),
						payload.y(),
						payload.z(),
						payload.dimension(),
						payload.isContainer()
				);
			});
		});
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
			PacketByteBuf buf = PacketByteBufs.create();
			sender.sendPacket(HANDSHAKE_CHANNEL, buf);
		});
		ServerLoginNetworking.registerGlobalReceiver(HANDSHAKE_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
			if (!understood) {
				handler.disconnect(Text.literal("This server requires the Polym Anti-Thief mod."));
				return;
			}

			boolean clientAccepted = buf.readBoolean();

			if (!clientAccepted) {
				handler.disconnect(Text.literal("Handshake failed: invalid client response."));
			}
		});
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) return ActionResult.PASS;
			if (!player.getStackInHand(hand).isOf(Blocks.HOPPER.asItem())) return ActionResult.PASS;

			BlockPos clickedPos = hitResult.getBlockPos();
			Direction side = hitResult.getSide();
			BlockPos placePos = clickedPos.offset(side);
			BlockPos above = placePos.up();
			BlockEntity beAbove = world.getBlockEntity(above);

			if (beAbove instanceof ShulkerBoxBlockEntity shulker && shulker.hasCustomName()) {
				if (shulker.hasCustomName()) {
					String shulkerName = shulker.getCustomName().getString();
					String playerName = player.getName().getString();
					Pattern pattern = Pattern.compile("\\+(\\w+)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(shulkerName);

					while (matcher.find()) {
						String nameInBox = matcher.group(1);
						if (playerName.equalsIgnoreCase(nameInBox)) {
							return ActionResult.PASS;
						}
					}
					String dimension = player.getWorld().getRegistryKey().getValue().toString();
					Float x = (float) player.getX();
					Float y = (float) player.getY();
					Float z = (float) player.getZ();
					DatabaseManager.insertLog(
							playerName,
							"placed-hopper",
							"",
							shulkerName,
							x,
							y,
							z,
							dimension,
							false
					);
				}
			}
			return ActionResult.PASS;
		});
	}
}