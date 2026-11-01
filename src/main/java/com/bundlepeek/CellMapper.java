package com.bundlepeek;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Collega gli oggetti "visti" mentre il gioco disegna il suggerimento agli oggetti reali del bundle.
 * Cosi' non serve conoscere il layout della finestra: valgono le posizioni in cui e' stato disegnato.
 */
public final class CellMapper {
    public record Captured<T>(T item, int x, int y) {}

    /** index = posizione dell'oggetto nel contenuto del bundle; x,y = dove e' stato disegnato. */
    public record Cell(int index, int x, int y) {}

    private CellMapper() {}

    public static <T> List<Cell> map(List<Captured<T>> captured, List<T> contents, BiPredicate<T, T> same) {
        boolean[] used = new boolean[contents.size()];
        List<Cell> cells = new ArrayList<>();
        for (Captured<T> c : captured) {
            for (int i = 0; i < contents.size(); i++) {
                if (!used[i] && same.test(c.item(), contents.get(i))) {
                    used[i] = true;
                    cells.add(new Cell(i, c.x(), c.y()));
                    break;
                }
            }
        }
        return cells;
    }

    /** Indice dell'oggetto sotto il mouse (casella 18x18, oggetto disegnato 1 pixel all'interno), o -1. */
    public static int cellAt(List<Cell> cells, double mx, double my) {
        for (Cell c : cells) {
            int x = c.x() - 1;
            int y = c.y() - 1;
            if (mx >= x && mx < x + PanelGeometry.CELL && my >= y && my < y + PanelGeometry.CELL) {
                return c.index();
            }
        }
        return -1;
    }
}
