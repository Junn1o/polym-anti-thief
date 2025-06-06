package com.junnio.mixin;

import com.junnio.util.PlayerDataCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(CactusBlock.class)
public class CactusShulkerBox {

    @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    private void preventShulkerBoxDeletion(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof ItemEntity item)) return;

        ItemStack stack = item.getStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        Text customName = stack.getCustomName();
        if (customName == null) return;
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            MinecraftServer server = serverWorld.getServer();
            String customNameText = stack.getCustomName().getString();
            Matcher matcher = Pattern.compile("\\+(\\w+)").matcher(customNameText);
            if (matcher.find()) {
                String playerName = matcher.group(1);
                if (PlayerDataCache.hasPlayerData(playerName, server)) {
                    ci.cancel();
                    item.setVelocity(0, 0.1, 0);
                    item.setPosition(item.getX(), item.getY() + 0.1, item.getZ());
                }
            }
        }
    }
}

