package com.bundlepeek;

import java.util.List;

/** Sceglie lo slot vuoto di appoggio usato per dividere una pila o spostarla con Shift+clic. */
public final class SlotChooser {
    /** zone: 0 = barra rapida, 1 = inventario, 2 = equipaggiamento/crafting (mai usato), 3 = contenitore esterno. */
    public record SlotInfo(int id, int zone, boolean empty, boolean enabled, boolean canInsert) {}

    public static final int EXCLUDED_ZONE = 2;

    private SlotChooser() {}

    /**
     * @param sameZoneOnly se true usa solo slot della stessa zona del bundle: serve per Shift+clic, perche'
     *                     il gioco sposta uno slot "dall'altra parte" rispetto a dove si trova.
     * @return id dello slot scelto, oppure -1 se non ce n'e' uno adatto
     */
    public static int choose(List<SlotInfo> slots, int bundleId, int bundleZone, boolean sameZoneOnly) {
        int fallback = -1;
        for (SlotInfo s : slots) {
            if (s.id() == bundleId || !s.enabled() || !s.empty() || !s.canInsert() || s.zone() == EXCLUDED_ZONE) {
                continue;
            }
            if (s.zone() == bundleZone) {
                return s.id();
            }
            if (fallback == -1) {
                fallback = s.id();
            }
        }
        return sameZoneOnly ? -1 : fallback;
    }
}
