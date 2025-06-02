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
        ScreenHandler handler = (ScreenHandler) (Object) this;

        if (slotId < 0 || slotId >= handler.slots.size()) return;
        Slot slot = handler.getSlot(slotId);
        if (slot == null || slot.inventory == player.getInventory()) return;

        String playerName = player.getName().getString();
        String lowerPlayerName = playerName.toLowerCase();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());

        boolean isValidAction = switch (actionType) {
            case PICKUP, QUICK_MOVE, PICKUP_ALL, THROW, SWAP, CLONE, QUICK_CRAFT -> true;
        };

        if (!isValidAction) return;

        // 1. Shulker taken from normal containers (chest, barrel, hopper...)
        if (handler instanceof GenericContainerScreenHandler ||
                handler instanceof HopperScreenHandler ||
                handler instanceof Generic3x3ContainerScreenHandler) {

            if (!slot.getStack().isEmpty()) {
                ItemStack stack = slot.getStack();
                if (stack.getItem() instanceof BlockItem blockItem &&
                        blockItem.getBlock() instanceof ShulkerBoxBlock) {

                    Text customName = stack.getCustomName();
                    String name = (customName != null) ? customName.getString() : stack.getName().getString();
                    String lowerName = name.toLowerCase();

                    if (lowerName.contains("+") && !lowerName.endsWith("+" + lowerPlayerName)) {
                        ModNetworking.sendShulkerLogPacket(playerName, name, pos, dimension, true, "taken", "");
                    }
                }
            }

            // 2. Items taken from shulker box GUIs
        } else if (handler instanceof ShulkerBoxScreenHandler) {
            Screen screen = MinecraftClient.getInstance().currentScreen;
            if (screen instanceof HandledScreen<?> handledScreen) {
                String containerName = handledScreen.getTitle().getString();
                String lowerContainerName = containerName.toLowerCase();

                if (lowerContainerName.contains("+") && !lowerContainerName.endsWith("+" + lowerPlayerName)) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        String itemName = stack.getName().getString();
                        String actionName = switch (actionType) {
                            case QUICK_MOVE -> "shift-clicked";
                            case PICKUP -> "picked up";
                            case PICKUP_ALL -> "picked up all";
                            case THROW -> "threw";
                            case SWAP -> "swapped";
                            case CLONE -> "cloned";
                            case QUICK_CRAFT -> "quick-crafted";
                        };
                        ModNetworking.sendShulkerLogPacket(playerName, containerName, pos, dimension, true, actionName, itemName);
                    }
                }
            }
        }
    }
}

