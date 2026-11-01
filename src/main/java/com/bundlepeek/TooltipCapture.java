package com.bundlepeek;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

/**
 * Registra gli oggetti che il gioco disegna mentre viene disegnato il suggerimento del bundle,
 * con le coordinate esatte. Attivo solo durante la nostra chiamata di disegno.
 */
public final class TooltipCapture {
    private static boolean active;
    private static final List<CellMapper.Captured<ItemStack>> ITEMS = new ArrayList<>();

    private TooltipCapture() {}

    public static void begin() {
        ITEMS.clear();
        active = true;
    }

    public static List<CellMapper.Captured<ItemStack>> end() {
        active = false;
        return new ArrayList<>(ITEMS);
    }

    /** Chiamato dalla mixin ogni volta che il gioco disegna un oggetto. */
    public static void record(ItemStack stack, int x, int y) {
        if (!active || stack == null || stack.isEmpty()) {
            return;
        }
        for (CellMapper.Captured<ItemStack> c : ITEMS) {
            if (c.x() == x && c.y() == y) {
                return; // gia' registrato (piu' funzioni di disegno si richiamano tra loro)
            }
        }
        ITEMS.add(new CellMapper.Captured<>(stack, x, y));
    }
}
