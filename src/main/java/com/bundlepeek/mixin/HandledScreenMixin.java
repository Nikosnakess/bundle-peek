package com.bundlepeek.mixin;

import com.bundlepeek.BundlePanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mentre la finestra del bundle e' aperta, non disegna il suggerimento vanilla (altrimenti si sovrapporrebbero). */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void bundlepeek$hideVanillaTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (BundlePanel.isActive()) {
            ci.cancel();
        }
    }
}
