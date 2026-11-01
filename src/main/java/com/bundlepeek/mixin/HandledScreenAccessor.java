package com.bundlepeek.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Posizione in cui e' disegnata la schermata (serve per sapere dove sono gli slot). */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int bundlepeek$getX();

    @Accessor("y")
    int bundlepeek$getY();
}
