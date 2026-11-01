package com.bundlepeek;

/**
 * Geometria della finestra (nessuna dipendenza da Minecraft, quindi testabile da sola).
 * Coordinate in pixel della GUI.
 */
public final class PanelGeometry {
    public static final int CELL = 18;      // lato di una casella degli slot
    public static final int GAP = 6;        // distanza tra lo slot e il contenuto della finestra
    public static final int MARGIN = 4;     // margine minimo dai bordi dello schermo
    public static final int TOLERANCE = 8;  // tolleranza attorno alla finestra (cornice + un po' di margine)

    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean contains(double px, double py) {
            return px >= x && px < x + w && py >= y && py < y + h;
        }
        public Rect expand(int e) { return new Rect(x - e, y - e, w + 2 * e, h + 2 * e); }
    }

    private PanelGeometry() {}

    /** Posiziona il contenuto: a destra dello slot (a sinistra se non c'e' spazio), allineato in alto, dentro lo schermo. */
    public static Rect placeContent(Rect cell, int w, int h, int screenW, int screenH) {
        int x = cell.right() + GAP;
        if (x + w > screenW - MARGIN) {
            int left = cell.x() - GAP - w;
            x = left >= MARGIN ? left : Math.max(MARGIN, screenW - MARGIN - w);
        }
        int y = cell.y();
        if (y + h > screenH - MARGIN) {
            y = screenH - MARGIN - h;
        }
        if (y < MARGIN) {
            y = MARGIN;
        }
        return new Rect(x, y, w, h);
    }

    /**
     * La finestra resta aperta finche' il mouse e' sullo slot, sulla finestra (con un po' di tolleranza)
     * o nel passaggio tra i due.
     */
    public static boolean keepOpen(Rect cell, Rect content, double mx, double my) {
        if (cell.contains(mx, my)) {
            return true;
        }
        if (content == null) {
            return false;
        }
        Rect area = content.expand(TOLERANCE);
        if (area.contains(mx, my)) {
            return true;
        }
        // passaggio orizzontale tra slot e finestra, alla stessa altezza dello slot
        if (area.x() >= cell.right()) {
            return new Rect(cell.right(), cell.y(), area.x() - cell.right(), cell.h()).contains(mx, my);
        }
        if (area.right() <= cell.x()) {
            return new Rect(area.right(), cell.y(), cell.x() - area.right(), cell.h()).contains(mx, my);
        }
        return false;
    }

    /** Zona in cui un clic viene "assorbito" dalla finestra invece di andare agli slot sottostanti. */
    public static Rect hitArea(Rect content) {
        return content.expand(4);
    }
}
