package com.junnio.mixin;

import com.junnio.util.PlayerDataCache;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperMinecartEntity.class)
public class blockShulkerBoxPull {

	@Inject(method = "canOperate", at = @At("HEAD"), cancellable = true)
	private void blockShulkerBoxPull(CallbackInfoReturnable<Boolean> cir) {
		HopperMinecartEntity self = (HopperMinecartEntity) (Object) this;
		World world = self.getWorld();
		if (!(world instanceof ServerWorld serverWorld)) return;
		MinecraftServer server = serverWorld.getServer();
		BlockPos above = self.getBlockPos().up();
		BlockEntity be = world.getBlockEntity(above);

		if (be instanceof ShulkerBoxBlockEntity shulker && shulker.hasCustomName()) {
			String customName = shulker.getCustomName().getString();
			if (customName.startsWith("+")) {
				String playerName = customName.substring(1).split("[^\\w]")[0];

				if (PlayerDataCache.hasPlayerData(playerName, server)) {
					cir.setReturnValue(false);
				}
			}
		}
	}
}