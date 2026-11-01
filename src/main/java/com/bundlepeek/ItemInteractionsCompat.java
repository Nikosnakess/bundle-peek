package com.bundlepeek;

import java.lang.reflect.Method;

/**
 * Compatibilita' con la libreria "Item Interactions" (inclusa in MetalBundles): quella libreria sceglie
 * l'oggetto da prelevare usando una propria selezione, diversa da quella del gioco.
 * La impostiamo solo se la libreria c'e', tramite reflection, quindi la mod funziona anche senza.
 */
final class ItemInteractionsCompat {
    private static boolean resolved;
    private static Method setter;

    private ItemInteractionsCompat() {}

    static void setCurrentSlot(Object player, int index) {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> helper = Class.forName("fuzs.iteminteractions.impl.world.inventory.ContainerSlotHelper");
                for (Method m : helper.getMethods()) {
                    if (m.getName().equals("setCurrentContainerSlot") && m.getParameterCount() == 2) {
                        setter = m;
                    }
                }
            } catch (Throwable ignored) {
                // libreria non presente: nessun problema
            }
        }
        if (setter != null) {
            try {
                setter.invoke(null, player, index);
            } catch (Throwable ignored) {
                // cambiata internamente: restiamo con la selezione del gioco
            }
        }
    }
}
