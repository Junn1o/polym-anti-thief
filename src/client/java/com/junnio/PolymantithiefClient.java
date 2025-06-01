package com.junnio;

import com.junnio.net.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class PolymantithiefClient implements ClientModInitializer {
	public static final String MOD_ID = "polym-anti-thief";

	@Override
	public void onInitializeClient() {
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (world.isClient) return ActionResult.PASS;

			// Only care about placing hoppers
			if (!player.getStackInHand(hand).isOf(Blocks.HOPPER.asItem())) return ActionResult.PASS;

			BlockPos clickedPos = hitResult.getBlockPos();
			Direction side = hitResult.getSide();
			BlockPos placePos = clickedPos.offset(side); // Hopper will be placed here

			// Check if there's a Shulker Box above the place position
			BlockPos above = placePos.up();
			BlockEntity beAbove = world.getBlockEntity(above);
			if (beAbove instanceof ShulkerBoxBlockEntity) {
				player.sendMessage(Text.of("You are placing a hopper under a shulker box!"), false);
			}

			return ActionResult.PASS;
		});
		ModNetworking.init();
	}

}