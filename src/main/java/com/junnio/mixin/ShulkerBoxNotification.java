package com.junnio.mixin;

import com.junnio.Polymantithief;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ShulkerBoxNotification {

    private static final Logger LOGGER = LoggerFactory.getLogger(Polymantithief.MOD_ID);

    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void onPlayerPickup(PlayerEntity player, CallbackInfo ci) {
        if (!player.getWorld().isClient()) {

            ItemEntity itemEntity = (ItemEntity)(Object)this;
            if (itemEntity.isRemoved()) {
                ItemStack stack = itemEntity.getStack();
                if (stack.getItem() instanceof BlockItem blockItem &&
                        blockItem.getBlock() instanceof ShulkerBoxBlock) {

                    Text customName = stack.getCustomName();
                    if (customName != null) {
                        String name = customName.getString();
                        String playerName = player.getName().getString();

                        if (!name.endsWith("+" + playerName)) {
                            double x = itemEntity.getX();
                            double y = itemEntity.getY();
                            double z = itemEntity.getZ();

                            String pos = String.format("(%.2f, %.2f, %.2f)", x, y, z);
                            String dimension = player.getWorld().getRegistryKey().getValue().toString();
                            LOGGER.info("{} "+"borrow"+" someone else's Shulker: {} at {} in {}",
                                    playerName, name, pos, dimension);
                        }
                    }
                }
            }
        }
    }
}
