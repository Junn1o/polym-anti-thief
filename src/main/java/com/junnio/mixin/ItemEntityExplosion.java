package com.junnio.mixin;
import com.junnio.util.PlayerDataCache;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityExplosion {

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void preventShulkerBoxExplosion(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity item = (ItemEntity) (Object) this;
        ItemStack stack = item.getStack();

        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;
        Text customName = stack.getCustomName();
        if (customName == null) return;
        String customNameText = stack.getCustomName().getString();
        if (!customNameText.startsWith("+")) return;

        String playerName = customNameText.substring(1).split("[^\\w]")[0];

        MinecraftServer server = world.getServer();

        if (source.isIn(DamageTypeTags.IS_EXPLOSION) &&
                PlayerDataCache.hasPlayerData(playerName, server)) {
            cir.setReturnValue(false);
        }
    }
}


