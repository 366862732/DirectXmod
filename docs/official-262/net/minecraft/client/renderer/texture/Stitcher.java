/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.StitcherException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class Stitcher<T extends Entry> {
    private static final Comparator<Holder<?>> HOLDER_COMPARATOR = Comparator.comparing(h -> -h.height).thenComparing(h -> -h.width).thenComparing(h -> h.entry.name());
    private final int mipLevel;
    private final List<Holder<T>> texturesToBeStitched = new ArrayList<Holder<T>>();
    private final List<Region<T>> storage = new ArrayList<Region<T>>();
    private int storageX;
    private int storageY;
    private final int maxWidth;
    private final int maxHeight;
    private final int padding;

    public Stitcher(int maxWidth, int maxHeight, int mipLevel, int anisotropyBit) {
        this.mipLevel = mipLevel;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.padding = 1 << mipLevel << Mth.clamp(anisotropyBit - 1, 0, 4);
    }

    public int getWidth() {
        return this.storageX;
    }

    public int getHeight() {
        return this.storageY;
    }

    public void registerSprite(T entry) {
        Holder<T> holder = new Holder<T>(entry, Stitcher.smallestFittingMinTexel(entry.width() + this.padding * 2, this.mipLevel), Stitcher.smallestFittingMinTexel(entry.height() + this.padding * 2, this.mipLevel));
        this.texturesToBeStitched.add(holder);
    }

    public void stitch() {
        ArrayList<Holder<T>> holders = new ArrayList<Holder<T>>(this.texturesToBeStitched);
        holders.sort(HOLDER_COMPARATOR);
        for (Holder holder : holders) {
            if (this.addToStorage(holder)) continue;
            throw new StitcherException((Entry)holder.entry, (Collection)holders.stream().map(h -> h.entry).collect(ImmutableList.toImmutableList()));
        }
    }

    public void gatherSprites(SpriteLoader<T> loader) {
        for (Region<T> topRegion : this.storage) {
            topRegion.walk(loader, this.padding);
        }
    }

    private static int smallestFittingMinTexel(int input, int maxMipLevel) {
        return (input >> maxMipLevel) + ((input & (1 << maxMipLevel) - 1) == 0 ? 0 : 1) << maxMipLevel;
    }

    private boolean addToStorage(Holder<T> holder) {
        for (Region<T> region : this.storage) {
            if (!region.add(holder)) continue;
            return true;
        }
        return this.expand(holder);
    }

    private boolean expand(Holder<T> holder) {
        Region<T> slot;
        boolean growOnX;
        boolean yWillGrow;
        boolean yCanGrow;
        int xCurrentSize = Mth.smallestEncompassingPowerOfTwo(this.storageX);
        int yCurrentSize = Mth.smallestEncompassingPowerOfTwo(this.storageY);
        int xNewSize = Mth.smallestEncompassingPowerOfTwo(this.storageX + holder.width);
        int yNewSize = Mth.smallestEncompassingPowerOfTwo(this.storageY + holder.height);
        boolean xCanGrow = xNewSize <= this.maxWidth;
        boolean bl = yCanGrow = yNewSize <= this.maxHeight;
        if (!xCanGrow && !yCanGrow) {
            return false;
        }
        boolean xWillGrow = xCanGrow && xCurrentSize != xNewSize;
        boolean bl2 = yWillGrow = yCanGrow && yCurrentSize != yNewSize;
        if (xWillGrow ^ yWillGrow) {
            growOnX = xWillGrow;
        } else {
            boolean bl3 = growOnX = xCanGrow && xCurrentSize <= yCurrentSize;
        }
        if (growOnX) {
            if (this.storageY == 0) {
                this.storageY = yNewSize;
            }
            slot = new Region(this.storageX, 0, xNewSize - this.storageX, this.storageY);
            this.storageX = xNewSize;
        } else {
            slot = new Region<T>(0, this.storageY, this.storageX, yNewSize - this.storageY);
            this.storageY = yNewSize;
        }
        slot.add(holder);
        this.storage.add(slot);
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    private record Holder<T extends Entry>(T entry, int width, int height) {
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Entry {
        public int width();

        public int height();

        public Identifier name();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Region<T extends Entry> {
        private final int originX;
        private final int originY;
        private final int width;
        private final int height;
        private @Nullable List<Region<T>> subSlots;
        private @Nullable Holder<T> holder;

        public Region(int originX, int originY, int width, int height) {
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return this.originX;
        }

        public int getY() {
            return this.originY;
        }

        public boolean add(Holder<T> holder) {
            if (this.holder != null) {
                return false;
            }
            int textureWidth = holder.width;
            int textureHeight = holder.height;
            if (textureWidth > this.width || textureHeight > this.height) {
                return false;
            }
            if (textureWidth == this.width && textureHeight == this.height) {
                this.holder = holder;
                return true;
            }
            if (this.subSlots == null) {
                this.subSlots = new ArrayList<Region<T>>(1);
                this.subSlots.add(new Region<T>(this.originX, this.originY, textureWidth, textureHeight));
                int spareWidth = this.width - textureWidth;
                int spareHeight = this.height - textureHeight;
                if (spareHeight > 0 && spareWidth > 0) {
                    int bottom;
                    int right = Math.max(this.height, spareWidth);
                    if (right >= (bottom = Math.max(this.width, spareHeight))) {
                        this.subSlots.add(new Region<T>(this.originX, this.originY + textureHeight, textureWidth, spareHeight));
                        this.subSlots.add(new Region<T>(this.originX + textureWidth, this.originY, spareWidth, this.height));
                    } else {
                        this.subSlots.add(new Region<T>(this.originX + textureWidth, this.originY, spareWidth, textureHeight));
                        this.subSlots.add(new Region<T>(this.originX, this.originY + textureHeight, this.width, spareHeight));
                    }
                } else if (spareWidth == 0) {
                    this.subSlots.add(new Region<T>(this.originX, this.originY + textureHeight, textureWidth, spareHeight));
                } else if (spareHeight == 0) {
                    this.subSlots.add(new Region<T>(this.originX + textureWidth, this.originY, spareWidth, textureHeight));
                }
            }
            for (Region<T> subSlot : this.subSlots) {
                if (!subSlot.add(holder)) continue;
                return true;
            }
            return false;
        }

        public void walk(SpriteLoader<T> output, int padding) {
            if (this.holder != null) {
                output.load(this.holder.entry, this.getX(), this.getY(), padding);
            } else if (this.subSlots != null) {
                for (Region subSlot : this.subSlots) {
                    subSlot.walk(output, padding);
                }
            }
        }

        public String toString() {
            return "Slot{originX=" + this.originX + ", originY=" + this.originY + ", width=" + this.width + ", height=" + this.height + ", texture=" + String.valueOf(this.holder) + ", subSlots=" + String.valueOf(this.subSlots) + "}";
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface SpriteLoader<T extends Entry> {
        public void load(T var1, int var2, int var3, int var4);
    }
}

