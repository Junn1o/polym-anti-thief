package com.junnio.mixin.client;

import com.junnio.ModNetworking;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.junnio.Polymantithief.LOGGER;

@Mixin(ScreenHandler.class)
public abstract class ShulkerBoxGuiPickupMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onShulkerTakenFromContainer(
            int slotId,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ){
        if (player.getWorld().isClient()) return;

        ScreenHandler handler = (ScreenHandler)(Object) this;

        // Allow only chest, barrel, hopper, dispenser, dropper
        if ((handler instanceof GenericContainerScreenHandler ||
                handler instanceof HopperScreenHandler ||
                handler instanceof Generic3x3ContainerScreenHandler)) {
            if (slotId >= 0 && slotId < handler.slots.size()) {
                Slot slot = handler.getSlot(slotId);

                // Ignore player inventory
                if (slot != null && slot.inventory != player.getInventory()) {

                    // Only care if taking items out
                    if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                        ItemStack stack = slot.getStack();

                        if (!stack.isEmpty() &&
                                stack.getItem() instanceof BlockItem blockItem &&
                                blockItem.getBlock() instanceof ShulkerBoxBlock) {

                            Text customName = stack.getCustomName();
                            String playerName = player.getName().getString();
                            String name = customName != null ? customName.getString() : stack.getName().getString();

                            if (!name.endsWith("+" + playerName) && name.contains("+")) {
                                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                                String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());
                                boolean isContainer = true;
                                ModNetworking.sendShulkerLogPacket(playerName, name, pos, dimension, isContainer);
                            }
                        }
                    }
                }
            }
        }
        else if (handler instanceof ShulkerBoxScreenHandler) {
            if (slotId >= 0 && slotId < handler.slots.size()) {
                Slot clickedSlot = handler.getSlot(slotId);

                if (actionType == SlotActionType.QUICK_MOVE &&
                        clickedSlot.inventory != player.getInventory()) {
                    // For Shift+Click, items go directly to inventory
                    ItemStack stack = clickedSlot.getStack();
                    if (!stack.isEmpty()) {
                        LOGGER.info("{} quick-moved item {} from Shulker Box into inventory",
                                player.getName().getString(),
                                stack.getName().getString());
                    }
                } else if (clickedSlot.inventory == player.getInventory()) {
                    // Normal click placing into inventory
                    ItemStack stack = clickedSlot.getStack();
                    if (!stack.isEmpty() && actionType == SlotActionType.PICKUP) {
                        LOGGER.info("{} took item {} from Shulker Box into inventory",
                                player.getName().getString(),
                                stack.getName().getString());
                    }
                }
            }
        }

    }
}

