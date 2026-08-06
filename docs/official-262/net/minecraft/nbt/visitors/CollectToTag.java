/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import org.jspecify.annotations.Nullable;

public class CollectToTag
implements StreamTagVisitor {
    private final Deque<ContainerBuilder> containerStack = new ArrayDeque<ContainerBuilder>();

    public CollectToTag() {
        this.containerStack.addLast(new RootBuilder());
    }

    public @Nullable Tag getResult() {
        return this.containerStack.getFirst().build();
    }

    protected int depth() {
        return this.containerStack.size() - 1;
    }

    private void appendEntry(Tag instance) {
        this.containerStack.getLast().acceptValue(instance);
    }

    @Override
    public StreamTagVisitor.ValueResult visitEnd() {
        this.appendEntry(EndTag.INSTANCE);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(String value) {
        this.appendEntry(StringTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte value) {
        this.appendEntry(ByteTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(short value) {
        this.appendEntry(ShortTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int value) {
        this.appendEntry(IntTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long value) {
        this.appendEntry(LongTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(float value) {
        this.appendEntry(FloatTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(double value) {
        this.appendEntry(DoubleTag.valueOf(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(byte[] value) {
        this.appendEntry(new ByteArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(int[] value) {
        this.appendEntry(new IntArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visit(long[] value) {
        this.appendEntry(new LongArrayTag(value));
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitList(TagType<?> elementType, int size) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.EntryResult visitElement(TagType<?> type, int index) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        return StreamTagVisitor.EntryResult.ENTER;
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String id) {
        this.containerStack.getLast().acceptKey(id);
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.EntryResult.ENTER;
    }

    private void enterContainerIfNeeded(TagType<?> type) {
        if (type == ListTag.TYPE) {
            this.containerStack.addLast(new ListBuilder());
        } else if (type == CompoundTag.TYPE) {
            this.containerStack.addLast(new CompoundBuilder());
        }
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        ContainerBuilder container = this.containerStack.removeLast();
        Tag tag = container.build();
        if (tag != null) {
            this.containerStack.getLast().acceptValue(tag);
        }
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> type) {
        this.enterContainerIfNeeded(type);
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    private static class RootBuilder
    implements ContainerBuilder {
        private @Nullable Tag result;

        private RootBuilder() {
        }

        @Override
        public void acceptValue(Tag tag) {
            this.result = tag;
        }

        @Override
        public @Nullable Tag build() {
            return this.result;
        }
    }

    private static interface ContainerBuilder {
        default public void acceptKey(String id) {
        }

        public void acceptValue(Tag var1);

        public @Nullable Tag build();
    }

    private static class ListBuilder
    implements ContainerBuilder {
        private final ListTag list = new ListTag();

        private ListBuilder() {
        }

        @Override
        public void acceptValue(Tag tag) {
            this.list.addAndUnwrap(tag);
        }

        @Override
        public Tag build() {
            return this.list;
        }
    }

    private static class CompoundBuilder
    implements ContainerBuilder {
        private final CompoundTag compound = new CompoundTag();
        private String lastId = "";

        private CompoundBuilder() {
        }

        @Override
        public void acceptKey(String id) {
            this.lastId = id;
        }

        @Override
        public void acceptValue(Tag tag) {
            this.compound.put(this.lastId, tag);
        }

        @Override
        public Tag build() {
            return this.compound;
        }
    }
}

