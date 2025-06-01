package com.junnio.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperMinecartEntity.class)
public class ExampleMixin {
	@Inject(method = "canOperate", at = @At("HEAD"), cancellable = true)
	private void blockShulkerBoxPull(CallbackInfoReturnable<Boolean> cir) {
		HopperMinecartEntity self = (HopperMinecartEntity) (Object) this;

		BlockPos above = self.getBlockPos().up();
		BlockEntity be = self.getWorld().getBlockEntity(above);

		if (be instanceof ShulkerBoxBlockEntity shulker) {
			// Check if the shulker box has a custom name
			if (shulker.hasCustomName()) {
				String customName = shulker.getCustomName().getString();

				if ("NoPull".equalsIgnoreCase(customName)) {
					// Prevent item pulling only if name matches "NoPull"
					cir.setReturnValue(false);
				}
			}
		}
	}
}