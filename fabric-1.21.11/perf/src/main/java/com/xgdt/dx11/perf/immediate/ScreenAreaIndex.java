package com.xgdt.dx11.perf.immediate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * Coarse spatial index used by the GUI layer lookup.
 *
 * <p>The GUI render state asks each layer "is anything of yours inside this
 * rectangle?" while it decides where a newly submitted element belongs.  A
 * linear scan is wasteful once a layer holds a few dozen widgets.  We keep an
 * aggregate bounding box for an O(1) rejection and a 32-pixel hash grid for the
 * remaining cases.  Elements that span many cells (full-screen backgrounds,
 * for example) go into a short always-checked list instead of being written to
 * thousands of buckets.</p>
 *
 * <p>All coordinates are pre-scaled GUI coordinates and therefore fit in an
 * {@code int}; the grid key packs two cells into one long.</p>
 */
final class ScreenAreaIndex {
    private static final int CELL_SHIFT = 5;
    private static final int CELL_SIZE = 1 << CELL_SHIFT;
    private static final int MAX_CELLS_PER_AREA = 64;
    private static final int MAX_QUERY_CELLS = 256;

    private final List<ScreenRectangle> all = new ArrayList<>();
    private final List<ScreenRectangle> oversized = new ArrayList<>();
    private final Map<Long, List<ScreenRectangle>> buckets = new HashMap<>();

    private int minLeft = Integer.MAX_VALUE;
    private int minTop = Integer.MAX_VALUE;
    private int maxRight = Integer.MIN_VALUE;
    private int maxBottom = Integer.MIN_VALUE;

    void add(ScreenRectangle area) {
        this.all.add(area);
        this.minLeft = Math.min(this.minLeft, area.left());
        this.minTop = Math.min(this.minTop, area.top());
        this.maxRight = Math.max(this.maxRight, area.right());
        this.maxBottom = Math.max(this.maxBottom, area.bottom());

        int column0 = Math.floorDiv(area.left(), CELL_SIZE);
        int column1 = Math.floorDiv(area.right() - 1, CELL_SIZE);
        int row0 = Math.floorDiv(area.top(), CELL_SIZE);
        int row1 = Math.floorDiv(area.bottom() - 1, CELL_SIZE);
        long cells = (long) (column1 - column0 + 1) * (long) (row1 - row0 + 1);
        if (cells > MAX_CELLS_PER_AREA) {
            this.oversized.add(area);
            return;
        }
        for (int row = row0; row <= row1; row++) {
            for (int column = column0; column <= column1; column++) {
                this.buckets.computeIfAbsent(key(column, row), ignored -> new ArrayList<>(2))
                    .add(area);
            }
        }
    }

    boolean intersects(ScreenRectangle query) {
        if (this.all.isEmpty()
            || query.left() >= this.maxRight
            || query.right() <= this.minLeft
            || query.top() >= this.maxBottom
            || query.bottom() <= this.minTop) {
            return false;
        }

        int column0 = Math.floorDiv(query.left(), CELL_SIZE);
        int column1 = Math.floorDiv(query.right() - 1, CELL_SIZE);
        int row0 = Math.floorDiv(query.top(), CELL_SIZE);
        int row1 = Math.floorDiv(query.bottom() - 1, CELL_SIZE);
        long cells = (long) (column1 - column0 + 1) * (long) (row1 - row0 + 1);

        if (cells > MAX_QUERY_CELLS) {
            // A very large query would visit more buckets than entries.
            return this.scanAll(query);
        }
        for (int row = row0; row <= row1; row++) {
            for (int column = column0; column <= column1; column++) {
                List<ScreenRectangle> bucket = this.buckets.get(key(column, row));
                if (bucket != null) {
                    for (int i = 0; i < bucket.size(); i++) {
                        if (bucket.get(i).intersects(query)) {
                            return true;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < this.oversized.size(); i++) {
            if (this.oversized.get(i).intersects(query)) {
                return true;
            }
        }
        return false;
    }

    private boolean scanAll(ScreenRectangle query) {
        for (int i = 0; i < this.all.size(); i++) {
            if (this.all.get(i).intersects(query)) {
                return true;
            }
        }
        return false;
    }

    private static long key(int column, int row) {
        return ((long) column << 32) ^ (row & 0xFFFFFFFFL);
    }
}
