package com.xgdt.dx11.perf.immediate;

import java.util.ArrayList;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.ScreenArea;

/**
 * An {@link ArrayList} of GUI screen areas that also maintains a spatial
 * index.
 *
 * <p>Vanilla's {@code GuiRenderState} stores per-layer element/item/text
 * lists and scans them linearly whenever it needs to know whether a new
 * element overlaps the layer.  The lists are created empty and only ever grow
 * through {@link #add}, which lets us index entries as they arrive.  Any other
 * structural change (sorting, removal, bulk add) bumps {@code modCount}; the
 * lookup mixin then falls back to the vanilla scan instead of trusting a stale
 * index.</p>
 *
 * <p>Rectangles whose bounds are {@code null} (some render states lack a
 * bounding box) are not indexed, matching vanilla's behaviour of ignoring
 * them during intersection tests.</p>
 */
public final class SpatialArrayList<E extends ScreenArea> extends ArrayList<E> {
    private final ScreenAreaIndex index = new ScreenAreaIndex();
    private int indexedModCount;

    @Override
    public boolean add(E element) {
        int expectedModCount = this.modCount;
        boolean added = super.add(element);
        if (!added) {
            return false;
        }
        // If the caller mutated the list in a way we do not index (addAll,
        // a concurrent modification), expectedModCount no longer matches and
        // the index stays marked stale until the list is rebuilt.
        if (this.indexedModCount == expectedModCount) {
            ScreenRectangle bounds = element.bounds();
            if (bounds != null) {
                this.index.add(bounds);
            }
            this.indexedModCount = this.modCount;
        }
        return true;
    }

    public boolean isIndexValid() {
        return this.indexedModCount == this.modCount;
    }

    public boolean intersects(ScreenRectangle query) {
        return this.index.intersects(query);
    }
}
