package com.junnio.mixin;

import com.junnio.util.PlayerDataCache;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(BlockPlacementDispenserBehavior.class)
public class DispenserBehavior {
    @Inject(method = "dispenseSilently", at = @At("HEAD"), cancellable = true)
    private void preventShulkerBoxPlacement(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        Text customName = stack.getCustomName();
        if (customName == null) return;
        String customNameText = stack.getCustomName().getString();
        Matcher matcher = Pattern.compile("\\+(\\w+)").matcher(customNameText);
        if (matcher.find()) {
            World world = pointer.world();
            MinecraftServer server = world.getServer();
            String playerName = matcher.group(1);
            if (!world.isClient && (PlayerDataCache.hasPlayerData(playerName, server))) {
                Direction direction = pointer.state().get(DispenserBlock.FACING);
                BlockPos pos = pointer.pos().offset(direction);
                cir.setReturnValue(stack);
                ItemEntity drop = new ItemEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack.copyWithCount(1));
                world.spawnEntity(drop);
                stack.decrement(1);
            }
        }

    }
}
