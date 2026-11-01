import com.bundlepeek.CellMapper;
import com.bundlepeek.CellMapper.Captured;
import com.bundlepeek.CellMapper.Cell;
import com.bundlepeek.PanelGeometry;
import com.bundlepeek.PanelGeometry.Rect;
import com.bundlepeek.SlotChooser;
import com.bundlepeek.SlotChooser.SlotInfo;
import java.util.List;

/** Test della logica (posizione finestra, mappatura delle caselle, scelta dello slot di appoggio). */
public class BundlePeekLogicTest {
    static int fails = 0;
    static void check(String name, boolean ok) {
        if (!ok) fails++;
        System.out.printf("%-66s %s%n", name, ok ? "OK" : "<-- FALLITO");
    }

    public static void main(String[] a) {
        // ---------------- posizione ----------------
        Rect cell = new Rect(100, 50, 18, 18);
        Rect r = PanelGeometry.placeContent(cell, 80, 70, 400, 300);
        check("a destra dello slot con distanza GAP", r.x() == 118 + PanelGeometry.GAP && r.y() == 50);
        Rect l = PanelGeometry.placeContent(new Rect(350, 50, 18, 18), 80, 70, 400, 300);
        check("senza spazio a destra passa a sinistra", l.right() == 350 - PanelGeometry.GAP);
        Rect b = PanelGeometry.placeContent(new Rect(100, 270, 18, 18), 80, 70, 400, 300);
        check("in basso viene alzata dentro lo schermo", b.bottom() == 300 - PanelGeometry.MARGIN);
        Rect t = PanelGeometry.placeContent(new Rect(10, 2, 18, 18), 80, 70, 400, 300);
        check("in alto non esce dallo schermo", t.y() >= PanelGeometry.MARGIN);
        Rect tiny = PanelGeometry.placeContent(new Rect(10, 10, 18, 18), 80, 400, 100, 100);
        check("finestra piu' grande dello schermo: y mai negativa", tiny.y() >= PanelGeometry.MARGIN);

        // ---------------- resta aperta / si chiude ----------------
        check("mouse sullo slot -> aperta", PanelGeometry.keepOpen(cell, r, 105, 55));
        check("mouse sulla finestra -> aperta", PanelGeometry.keepOpen(cell, r, r.x() + 10, r.y() + 10));
        boolean bridge = true;
        for (int x = 118; x <= r.x(); x++) bridge &= PanelGeometry.keepOpen(cell, r, x, 60);
        check("nel passaggio slot->finestra non c'e' nessun vuoto", bridge);
        check("mouse lontano -> si chiude", !PanelGeometry.keepOpen(cell, r, 300, 250));
        check("mouse sotto lo slot (altro slot) -> si chiude", !PanelGeometry.keepOpen(cell, r, 105, 100));
        check("senza finestra ancora disegnata conta solo lo slot",
                PanelGeometry.keepOpen(cell, null, 105, 55) && !PanelGeometry.keepOpen(cell, null, 130, 55));
        check("zona dei clic = contenuto + margine", PanelGeometry.hitArea(r).contains(r.x() - 3, r.y() - 3)
                && !PanelGeometry.hitArea(r).contains(r.x() - 6, r.y()));

        // ---------------- mappatura delle caselle ----------------
        List<String> contents = List.of("legno", "pietra", "mela");
        List<Captured<String>> inOrder = List.of(new Captured<>("legno", 10, 10), new Captured<>("pietra", 28, 10), new Captured<>("mela", 46, 10));
        List<Cell> m1 = CellMapper.map(inOrder, contents, String::equals);
        check("mappatura in ordine", m1.size() == 3 && m1.get(0).index() == 0 && m1.get(2).index() == 2);
        List<Captured<String>> reversed = List.of(new Captured<>("mela", 10, 10), new Captured<>("pietra", 28, 10), new Captured<>("legno", 46, 10));
        List<Cell> m2 = CellMapper.map(reversed, contents, String::equals);
        check("mappatura con ordine invertito: ogni casella trova il suo oggetto",
                CellMapper.cellAt(m2, 11, 11) == 2 && CellMapper.cellAt(m2, 47, 11) == 0);
        List<String> dup = List.of("legno", "legno", "mela");
        List<Captured<String>> capDup = List.of(new Captured<>("legno", 10, 10), new Captured<>("legno", 28, 10), new Captured<>("mela", 46, 10));
        List<Cell> m3 = CellMapper.map(capDup, dup, String::equals);
        check("oggetti uguali: indici diversi per caselle diverse",
                CellMapper.cellAt(m3, 11, 11) == 0 && CellMapper.cellAt(m3, 29, 11) == 1 && CellMapper.cellAt(m3, 47, 11) == 2);
        List<Cell> m4 = CellMapper.map(List.of(new Captured<>("sconosciuto", 10, 10)), contents, String::equals);
        check("oggetto non riconosciuto: ignorato", m4.isEmpty());
        check("casella: il pixel sopra/sinistra dell'oggetto e' ancora la casella", CellMapper.cellAt(m1, 9, 9) == 0);
        check("casella: bordo tra due caselle (18 pixel)", CellMapper.cellAt(m1, 26, 11) == 0 && CellMapper.cellAt(m1, 27, 11) == 1);
        check("fuori dalle caselle -> -1", CellMapper.cellAt(m1, 200, 200) == -1);

        // ---------------- slot di appoggio ----------------
        List<SlotInfo> slots = List.of(
                new SlotInfo(0, 3, true, true, false),   // uscita crafting: non inseribile
                new SlotInfo(5, 2, true, true, true),    // equipaggiamento: escluso
                new SlotInfo(9, 1, false, true, true),   // inventario, occupato
                new SlotInfo(10, 1, true, true, true),   // inventario libero
                new SlotInfo(36, 0, true, true, true),   // barra rapida libera
                new SlotInfo(50, 3, true, true, true));  // contenitore libero
        check("bundle nell'inventario: appoggio nell'inventario", SlotChooser.choose(slots, 12, 1, true) == 10);
        check("bundle nella barra rapida: appoggio nella barra rapida", SlotChooser.choose(slots, 40, 0, true) == 36);
        check("bundle nel baule: appoggio nel baule", SlotChooser.choose(slots, 60, 3, true) == 50);
        check("mai slot di equipaggiamento / uscita", SlotChooser.choose(slots, 12, 2, false) != 5 && SlotChooser.choose(slots, 12, 2, false) != 0);
        List<SlotInfo> noSame = List.of(new SlotInfo(9, 1, false, true, true), new SlotInfo(36, 0, true, true, true));
        check("Shift+clic senza posto nella zona giusta: nessuna scelta", SlotChooser.choose(noSame, 12, 1, true) == -1);
        check("dividere senza posto nella zona giusta: usa un'altra zona", SlotChooser.choose(noSame, 12, 1, false) == 36);
        check("slot disattivato ignorato", SlotChooser.choose(List.of(new SlotInfo(1, 1, true, false, true)), 12, 1, false) == -1);

        System.out.println(fails == 0 ? "\nTUTTI I TEST OK" : "\nTEST FALLITI: " + fails);
        if (fails > 0) System.exit(1);
    }
}
