package com.xgdt.dx11.perf.immediate.sign;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Render-target atlas that holds the rasterised text of many signs.
 *
 * <p>Vanilla renders every sign line as its own glyph quads every frame.  The
 * atlas lets a sign render once into a small rectangle and then be drawn as a
 * single textured quad until its text changes or it has been off-screen for a
 * few seconds.</p>
 *
 * <p>Slots are packed with a first-fit free-rectangle allocator.  The
 * reference mod's guillotine tree is deliberately not copied; first-fit is
 * easier to reason about and sign slot sizes are all close to each other
 * (four 9px lines plus a two pixel glow border at most), so the packing
 * difference does not matter in practice.</p>
 *
 * <p>All methods run on the render thread.</p>
 */
public final class SignAtlas extends TextureTarget implements AutoCloseable {
    /** A rectangle in top-down atlas coordinates. */
    public record Slot(int x, int y, int width, int height) {
    }

    private final int id;
    private final int atlasSize;
    private final Identifier identifier;
    private final List<Slot> free = new ArrayList<>();
    private final Set<Slot> used = new HashSet<>();
    private boolean closed;

    public SignAtlas(int id, int size) {
        super("gl4dx11 sign atlas " + id, size, size, true);
        this.id = id;
        this.atlasSize = size;
        this.identifier = Identifier.fromNamespaceAndPath("gl4dx11", "sign_atlas/" + id);
        this.free.add(new Slot(0, 0, size, size));
        Minecraft.getInstance().getTextureManager().register(this.identifier, new RenderTargetTexture(this));
        this.clearAll();
    }

    public int id() {
        return this.id;
    }

    public int atlasSize() {
        return this.atlasSize;
    }

    public Identifier identifier() {
        return this.identifier;
    }

    public int usedSlotCount() {
        return this.used.size();
    }

    /**
     * Reserves the first free rectangle that fits.
     *
     * @return the allocated slot, or {@code null} when the atlas is full or
     *         the request is larger than the atlas
     */
    public Slot allocate(int width, int height) {
        if (this.closed || width <= 0 || height <= 0
            || width > this.atlasSize || height > this.atlasSize) {
            return null;
        }

        for (int i = 0; i < this.free.size(); i++) {
            Slot rect = this.free.get(i);
            if (width > rect.width() || height > rect.height()) {
                continue;
            }
            this.free.remove(i);

            Slot slot = new Slot(rect.x(), rect.y(), width, height);
            int restWidth = rect.width() - width;
            int restHeight = rect.height() - height;
            if (restWidth > 0 && restHeight > 0) {
                if (restWidth >= restHeight) {
                    this.free.add(new Slot(rect.x() + width, rect.y(), restWidth, rect.height()));
                    this.free.add(new Slot(rect.x(), rect.y() + height, width, restHeight));
                } else {
                    this.free.add(new Slot(rect.x(), rect.y() + height, rect.width(), restHeight));
                    this.free.add(new Slot(rect.x() + width, rect.y(), restWidth, height));
                }
            } else if (restWidth > 0) {
                this.free.add(new Slot(rect.x() + width, rect.y(), restWidth, rect.height()));
            } else if (restHeight > 0) {
                this.free.add(new Slot(rect.x(), rect.y() + height, rect.width(), restHeight));
            }

            this.used.add(slot);
            return slot;
        }

        return null;
    }

    /**
     * Returns a slot to the free list and clears its pixels so a later,
     * smaller allocation cannot show glyphs from the previous owner through
     * transparent parts of the new text.
     */
    public void free(Slot slot) {
        if (this.closed || slot == null || !this.used.remove(slot)) {
            return;
        }
        this.free.add(slot);
        this.coalesce();
        this.clearRegion(slot);
    }

    public void clearAll() {
        if (this.closed) {
            return;
        }
        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(this.getColorTexture(), 0, this.getDepthTexture(), 1.0);
    }

    private void clearRegion(Slot slot) {
        // The backend accepts GL bottom-left rects; slots are stored in
        // top-down image order.
        int glY = this.atlasSize - slot.y() - slot.height();
        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(
                this.getColorTexture(),
                0,
                this.getDepthTexture(),
                1.0,
                slot.x(),
                glY,
                slot.width(),
                slot.height());
    }

    /** Merges neighbouring free rectangles until no more merges are possible. */
    private void coalesce() {
        boolean merged = true;
        while (merged) {
            merged = false;
            outer:
            for (int i = 0; i < this.free.size(); i++) {
                Slot a = this.free.get(i);
                for (int j = i + 1; j < this.free.size(); j++) {
                    Slot b = this.free.get(j);
                    Slot union = merge(a, b);
                    if (union != null) {
                        this.free.set(i, union);
                        this.free.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        }
    }

    private static Slot merge(Slot a, Slot b) {
        if (a.x() == b.x() && a.width() == b.width()) {
            if (a.y() + a.height() == b.y()) {
                return new Slot(a.x(), a.y(), a.width(), a.height() + b.height());
            }
            if (b.y() + b.height() == a.y()) {
                return new Slot(b.x(), b.y(), b.width(), a.height() + b.height());
            }
        }
        if (a.y() == b.y() && a.height() == b.height()) {
            if (a.x() + a.width() == b.x()) {
                return new Slot(a.x(), a.y(), a.width() + b.width(), a.height());
            }
            if (b.x() + b.width() == a.x()) {
                return new Slot(b.x(), b.y(), a.width() + b.width(), b.height());
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.used.clear();
        this.free.clear();
        Minecraft.getInstance().getTextureManager().release(this.identifier);
        this.destroyBuffers();
    }
}
