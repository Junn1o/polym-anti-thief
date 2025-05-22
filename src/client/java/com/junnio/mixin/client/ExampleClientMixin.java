package com.junnio.mixin.client;

import com.junnio.ModNetworking;
import com.junnio.Polymantithief;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ExampleClientMixin {
	@Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
	private void onItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.world == null || !client.isOnThread()) return;
		Entity collector = client.world.getEntityById(packet.getCollectorEntityId());
		Entity itemEntity = client.world.getEntityById(packet.getEntityId());

		if (collector == client.player && itemEntity instanceof ItemEntity item) {
			ItemStack stack = item.getStack();

			// Check if it's a Shulker Box
			if (stack.getItem() instanceof BlockItem blockItem &&
					blockItem.getBlock() instanceof ShulkerBoxBlock) {
				if (stack.getCustomName() != null) {
					String customName = stack.getCustomName().getString();
					int plus = customName.lastIndexOf('+');

					if (plus > -1 && plus < customName.length() - 1) {
						String owner = customName.substring(plus + 1).trim();
						String playerName = client.player.getName().getString().trim();

						if (!owner.equals(playerName)) {
							String pos = String.format("(%.2f, %.2f, %.2f)", item.getX(), item.getY(), item.getZ());
							String dimension = client.world.getRegistryKey().getValue().toString();
							boolean isContainer = false;
							ModNetworking.sendShulkerLogPacket(playerName, customName, pos, dimension, isContainer);
						}
					}
				}
			}
		}
	}
}