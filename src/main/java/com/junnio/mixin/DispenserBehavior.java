package com.junnio.mixin;

import net.minecraft.block.DispenserBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlacementDispenserBehavior.class)
public class DispenserBehavior {
    @Inject(method = "dispenseSilently", at = @At("HEAD"), cancellable = true)
    private void preventShulkerBoxPlacement(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        Text customNameText = stack.getCustomName();
        if (customNameText == null) return;

        World world = pointer.world();
        Direction direction = pointer.state().get(DispenserBlock.FACING);
        BlockPos pos = pointer.pos().offset(direction);

        cir.setReturnValue(stack);


        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d center = Vec3d.ofCenter(pos);

            PlayerEntity nearest = serverWorld.getClosestPlayer(center.x, center.y, center.z, 5.0, false);

            if (nearest != null) {
                boolean inserted = nearest.getInventory().insertStack(stack.copyWithCount(1));
                if (inserted) {
                    stack.decrement(1);
                    return; // Success
                }
            }

            ItemEntity drop = new ItemEntity(world, center.x, center.y, center.z, stack.copyWithCount(1));
            world.spawnEntity(drop);
            stack.decrement(1);
        }
    }
}
