package com.junnio.mixin.client;

import com.junnio.net.ModNetworking;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ShulkerBoxPickup {
	@Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
	private void onItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.world == null || !client.isOnThread()) return;

		Entity collector = client.world.getEntityById(packet.getCollectorEntityId());
		Entity itemEntity = client.world.getEntityById(packet.getEntityId());

		if (!(collector instanceof PlayerEntity player)) return;
		if (itemEntity instanceof ItemEntity item) {
			ItemStack stack = item.getStack();

			if (!(stack.getItem() instanceof BlockItem blockItem)) return;
			if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

			Text customNameText = stack.getCustomName();
			if (customNameText == null) return;

			String containerName = customNameText.getString();
			int plusIndex = containerName.lastIndexOf('+');
			if (plusIndex == -1 || plusIndex == containerName.length() - 1) return;

			String owner = containerName.substring(plusIndex + 1).trim();
			String playerName = player.getName().getString().trim();

			if (!owner.equalsIgnoreCase(playerName)) {
				Float x = (float) player.getX();
				Float y = (float) player.getY();
				Float z = (float) player.getZ();
				String dimension = client.world.getRegistryKey().getValue().toString();
				player.sendMessage(Text.literal("Tôi đã nhặt ShulkerBox §e"+containerName+" §fở dưới dất"),false);
				ModNetworking.sendShulkerLogPacket(playerName, containerName, x, y, z, dimension, false, "picked-up", "");
			}
		}
	}
}