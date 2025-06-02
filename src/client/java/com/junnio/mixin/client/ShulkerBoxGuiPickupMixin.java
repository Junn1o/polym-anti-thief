package com.junnio.mixin.client;

import com.junnio.net.ModNetworking;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ShulkerBoxGuiPickupMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"))
    private void onShulkerTakenFromContainer(
            int slotId,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        //if (player.getWorld().isClient()) return;

        ScreenHandler handler = (ScreenHandler) (Object) this;

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
                            String lowerPlayerName = playerName.toLowerCase();
                            String name = customName != null ? customName.getString() : stack.getName().getString();
                            String lowerName = name.toLowerCase();
                            if (!lowerName.endsWith("+" + lowerPlayerName) && lowerName.contains("+")) {
                                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                                String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());
                                ModNetworking.sendShulkerLogPacket(playerName, name, pos, dimension, false, "", "");
                            }
                        }
                    }
                }
            }
        } else if (handler instanceof ShulkerBoxScreenHandler shulkerHandler) {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof HandledScreen<?> handledScreen) {
                String containerName = handledScreen.getTitle().getString();
                String lowerCaseContainerName = containerName.toLowerCase();
                String playerName = player.getName().getString();
                String lowerCasePlayerName = playerName.toLowerCase();

                if (lowerCaseContainerName.contains("+") && !lowerCaseContainerName.endsWith("+" + lowerCasePlayerName)) {
                    if (slotId >= 0 && slotId < handler.slots.size()) {
                        Slot clickedSlot = handler.getSlot(slotId);
                        String actionName = switch (actionType) {
                            case QUICK_MOVE -> "shift-clicked";
                            case PICKUP -> "picked up";
                            case PICKUP_ALL -> "picked up all";
                            case THROW -> "threw";
                            case SWAP -> "swapped";
                            case CLONE -> "cloned";
                            case QUICK_CRAFT -> "quick-crafted";
                            default -> "unknown-action";
                        };

                        if ((actionType == SlotActionType.QUICK_MOVE
                                || actionType == SlotActionType.THROW
                                || actionType == SlotActionType.PICKUP
                                || actionType == SlotActionType.PICKUP_ALL
                                || actionType == SlotActionType.SWAP
                                || actionType == SlotActionType.CLONE
                                || actionType == SlotActionType.QUICK_CRAFT)
                                && clickedSlot.inventory != player.getInventory()) {

                            ItemStack stack = clickedSlot.getStack();
                            if (!stack.isEmpty()) {
                                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                                String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());
                                ModNetworking.sendShulkerLogPacket(playerName, containerName, pos, dimension, true, actionName, stack.getName().getString());
                            }
                        }
                    }
                }
            }
        }
    }
}

