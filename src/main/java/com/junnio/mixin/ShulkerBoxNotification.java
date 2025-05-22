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

@Mixin(ItemEntity.class)
public class ShulkerBoxNotification {

    private static final Logger LOGGER = LoggerFactory.getLogger(Polymantithief.MOD_ID);

    @Inject(method = "onPlayerCollision", at = @At("TAIL"))
    private void onPlayerPickup(PlayerEntity player) {
        if (player.getWorld().isClient()) return;

        ItemEntity itemEntity = (ItemEntity) (Object) this;

        // Only proceed if the item was picked up (entity removed)
        if (!itemEntity.isRemoved()) return;

        ItemStack stack = itemEntity.getStack();
        Item item = stack.getItem();

        // Check if it's a Shulker Box
        if (!(item instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

        Text customName = stack.getCustomName();
        if (customName == null) return;

        String name = customName.getString();
        String playerName = player.getName().getString();

        // Only log if the custom name does NOT end with "+playerName"
        if (name.endsWith("+" + playerName)) return;

        // Prepare log data
        String pos = String.format("(%.2f, %.2f, %.2f)", itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        String dimension = player.getWorld().getRegistryKey().getValue().toString();

        // Log to server console
        LOGGER.info("{} borrowed someone else's Shulker named: {} at {} in {}", playerName, name, pos, dimension);
    }
}
