package com.bundlepeek.mixin;

import com.bundlepeek.TooltipCapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Registra dove vengono disegnati gli oggetti del suggerimento (solo mentre disegniamo la nostra finestra). */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;III)V",
            at = @At("HEAD"))
    private void bundlepeek$captureItem(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed,
                                        CallbackInfo ci) {
        TooltipCapture.record(stack, x, y);
    }

    @Inject(method = "drawItemWithoutEntity(Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
    private void bundlepeek$captureItemWithoutEntity(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        TooltipCapture.record(stack, x, y);
    }
}
