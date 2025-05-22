package com.junnio.mixin.client;

import com.junnio.ModNetworking;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ScreenHandler.class)
public abstract class ShulkerBoxGuiPickupMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onShulkerTakenFromContainer(int slotId, SlotActionType actionType, PlayerEntity player) {
        if (!player.getWorld().isClient()) return;

        ScreenHandler handler = (ScreenHandler)(Object) this;

        // Allow only chest, barrel, hopper, dispenser, dropper
        boolean isContainer =
                handler instanceof GenericContainerScreenHandler ||
                        handler instanceof HopperScreenHandler ||
                        handler instanceof Generic3x3ContainerScreenHandler;

        if (!isContainer) return;

        // Make sure slot is within bounds
        if (slotId < 0 || slotId >= handler.slots.size()) return;

        Slot slot = handler.getSlot(slotId);

        // Ignore if slot is null or belongs to player inventory
        if (slot == null || slot.inventory == player.getInventory()) return;

        // Only care if item is being taken out
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        Item item = stack.getItem();
        if (!(item instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

        // Determine custom name
        Text customName = stack.getCustomName();
        if (customName == null) return;
        String playerName = player.getName().getString();
        String name = customName.getString();

        // Only report if name doesn't end with +playerName
        if (name.endsWith("+" + playerName)) return;

        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());

        ModNetworking.sendShulkerLogPacket(playerName, name, pos, dimension);
    }
}

