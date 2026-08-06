/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class RotatingSectionStorage<T extends Value>
implements Iterable<T> {
    private final Node<T>[] nodes;
    private final int radius;
    private final int minY;
    private final int maxY;
    private final int sectionGridSizeY;
    private final int sectionGridSizeXZ;
    private SectionPos centerSectionPos = SectionPos.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    public RotatingSectionStorage(int radius, int minY, int maxY, ValueCreator<T> valueCreator) {
        this.radius = radius;
        this.minY = minY;
        this.maxY = maxY;
        this.sectionGridSizeY = maxY - minY + 1;
        this.sectionGridSizeXZ = radius * 2 + 1;
        int totalSections = this.sectionGridSizeXZ * this.sectionGridSizeXZ * this.sectionGridSizeY;
        this.nodes = new Node[totalSections];
        for (int x = 0; x < this.sectionGridSizeXZ; ++x) {
            for (int y = 0; y < this.sectionGridSizeY; ++y) {
                for (int z = 0; z < this.sectionGridSizeXZ; ++z) {
                    int index = this.getSectionIndex(x, y, z);
                    long sectionNode = SectionPos.asLong(x, y + minY, z);
                    this.nodes[index] = new Node<T>(valueCreator.createValue(index, sectionNode));
                }
            }
        }
    }

    public boolean repositionCenter(SectionPos newCenterSectionPos) {
        if (newCenterSectionPos.equals(this.centerSectionPos)) {
            return false;
        }
        int lowestX = newCenterSectionPos.x() - this.radius;
        int lowestZ = newCenterSectionPos.z() - this.radius;
        for (int gridX = 0; gridX < this.sectionGridSizeXZ; ++gridX) {
            int newSectionX = lowestX + Math.floorMod(gridX - lowestX, this.sectionGridSizeXZ);
            for (int gridZ = 0; gridZ < this.sectionGridSizeXZ; ++gridZ) {
                int newSectionZ = lowestZ + Math.floorMod(gridZ - lowestZ, this.sectionGridSizeXZ);
                for (int gridY = 0; gridY < this.sectionGridSizeY; ++gridY) {
                    int newSectionY = this.minY + gridY;
                    Value value = (Value)this.nodes[this.getSectionIndex((int)gridX, (int)gridY, (int)gridZ)].value;
                    long sectionNode = value.getSectionNode();
                    if (sectionNode == SectionPos.asLong(newSectionX, newSectionY, newSectionZ)) continue;
                    value.setSectionNode(SectionPos.asLong(newSectionX, newSectionY, newSectionZ));
                }
            }
        }
        this.centerSectionPos = newCenterSectionPos;
        return true;
    }

    public int radius() {
        return this.radius;
    }

    public int minY() {
        return this.minY;
    }

    public int maxY() {
        return this.maxY;
    }

    public int height() {
        return this.sectionGridSizeY;
    }

    public SectionPos centerSectionPos() {
        return this.centerSectionPos;
    }

    public @Nullable T getValueAt(BlockPos pos) {
        return this.getValue(SectionPos.asLong(pos));
    }

    public @Nullable T getValue(long sectionNode) {
        int sectionX = SectionPos.x(sectionNode);
        int sectionY = SectionPos.y(sectionNode);
        int sectionZ = SectionPos.z(sectionNode);
        return this.getValue(sectionX, sectionY, sectionZ);
    }

    public @Nullable T getValue(int sectionX, int sectionY, int sectionZ) {
        if (!this.containsSection(sectionX, sectionY, sectionZ)) {
            return null;
        }
        int y = sectionY - this.minY;
        int x = Math.floorMod(sectionX, this.sectionGridSizeXZ);
        int z = Math.floorMod(sectionZ, this.sectionGridSizeXZ);
        return (T)((Value)this.nodes[this.getSectionIndex((int)x, (int)y, (int)z)].value);
    }

    private boolean containsSection(int sectionX, int sectionY, int sectionZ) {
        if (sectionY < this.minY || sectionY > this.maxY) {
            return false;
        }
        if (sectionX < this.centerSectionPos.x() - this.radius || sectionX > this.centerSectionPos.x() + this.radius) {
            return false;
        }
        return sectionZ >= this.centerSectionPos.z() - this.radius && sectionZ <= this.centerSectionPos.z() + this.radius;
    }

    private int getSectionIndex(int x, int y, int z) {
        return (z * this.sectionGridSizeY + y) * this.sectionGridSizeXZ + x;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>(this){
            private int i;
            final /* synthetic */ RotatingSectionStorage this$0;
            {
                RotatingSectionStorage rotatingSectionStorage = this$0;
                Objects.requireNonNull(rotatingSectionStorage);
                this.this$0 = rotatingSectionStorage;
            }

            @Override
            public boolean hasNext() {
                return this.i < this.this$0.nodes.length - 1;
            }

            @Override
            public T next() {
                if (this.i >= this.this$0.nodes.length) {
                    throw new NoSuchElementException();
                }
                return (Value)this.this$0.nodes[this.i++].value;
            }
        };
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (Node<T> node : this.nodes) {
            action.accept(node.value);
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(this.iterator(), (long)this.nodes.length, 0);
    }

    public int size() {
        return this.nodes.length;
    }

    @Environment(value=EnvType.CLIENT)
    private record Node<T>(T value) {
    }

    @Environment(value=EnvType.CLIENT)
    public static interface ValueCreator<T extends Value> {
        public T createValue(int var1, long var2);
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Value {
        public void setSectionNode(long var1);

        public long getSectionNode();
    }
}

