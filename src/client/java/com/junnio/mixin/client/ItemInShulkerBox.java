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
public class ItemInShulkerBox {
    @Inject(method = "onSlotClick", at = @At("RETURN"), cancellable = true)
    private void onShulkerTakenFromContainer(
            int slotId,
            int button,
            SlotActionType actionType,
            PlayerEntity player,
            CallbackInfo ci
    ) {
        if (player.getWorld().isClient()) return;

        ScreenHandler handler = (ScreenHandler)(Object) this;
        // We care only about picking up items (or quick move)
        if (actionType != SlotActionType.PICKUP && actionType != SlotActionType.QUICK_MOVE) return;

        // Check slot validity
        if (slotId < 0 || slotId >= handler.slots.size()) return;
        Slot slot = handler.getSlot(slotId);

        // Make sure slot is not player's inventory (we want container slots only)
        if (slot == null || slot.inventory == player.getInventory()) return;

        ItemStack stack = slot.getStack();

        // Check if the item is a Shulker Box
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            Text customName = stack.getCustomName();
            String playerName = player.getName().getString();
            String name = customName != null ? customName.getString() : stack.getName().getString();

            // Log only if name contains "+" but does NOT end with "+playerName"
            if (name.contains("+") && !name.endsWith("+" + playerName)) {
                String dimension = player.getWorld().getRegistryKey().getValue().toString();
                String pos = String.format("(%.2f, %.2f, %.2f)", player.getX(), player.getY(), player.getZ());
                boolean isContainer = true;
                ModNetworking.sendShulkerLogPacket(playerName, name, pos, dimension, isContainer);
            }
        }
    }
}
