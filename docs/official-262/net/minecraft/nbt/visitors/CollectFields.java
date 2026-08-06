/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 */
package net.minecraft.nbt.visitors;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.visitors.CollectToTag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.FieldTree;

public class CollectFields
extends CollectToTag {
    private int fieldsToGetCount;
    private final Set<TagType<?>> wantedTypes;
    private final Deque<FieldTree> stack = new ArrayDeque<FieldTree>();

    public CollectFields(FieldSelector ... wantedFields) {
        this.fieldsToGetCount = wantedFields.length;
        ImmutableSet.Builder wantedTypes = ImmutableSet.builder();
        FieldTree rootFrame = FieldTree.createRoot();
        for (FieldSelector wantedField : wantedFields) {
            rootFrame.addEntry(wantedField);
            wantedTypes.add(wantedField.type());
        }
        this.stack.push(rootFrame);
        wantedTypes.add(CompoundTag.TYPE);
        this.wantedTypes = wantedTypes.build();
    }

    @Override
    public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> type) {
        if (type != CompoundTag.TYPE) {
            return StreamTagVisitor.ValueResult.HALT;
        }
        return super.visitRootEntry(type);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        FieldTree currentFrame = this.stack.element();
        if (this.depth() > currentFrame.depth()) {
            return super.visitEntry(type);
        }
        if (this.fieldsToGetCount <= 0) {
            return StreamTagVisitor.EntryResult.BREAK;
        }
        if (!this.wantedTypes.contains(type)) {
            return StreamTagVisitor.EntryResult.SKIP;
        }
        return super.visitEntry(type);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String id) {
        FieldTree newFrame;
        FieldTree currentFrame = this.stack.element();
        if (this.depth() > currentFrame.depth()) {
            return super.visitEntry(type, id);
        }
        if (currentFrame.selectedFields().remove(id, type)) {
            --this.fieldsToGetCount;
            return super.visitEntry(type, id);
        }
        if (type == CompoundTag.TYPE && (newFrame = currentFrame.fieldsToRecurse().get(id)) != null) {
            this.stack.push(newFrame);
            return super.visitEntry(type, id);
        }
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        if (this.depth() == this.stack.element().depth()) {
            this.stack.pop();
        }
        return super.visitContainerEnd();
    }

    public int getMissingFieldCount() {
        return this.fieldsToGetCount;
    }
}

