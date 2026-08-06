/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt.visitors;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.visitors.CollectToTag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.FieldTree;

public class SkipFields
extends CollectToTag {
    private final Deque<FieldTree> stack = new ArrayDeque<FieldTree>();

    public SkipFields(FieldSelector ... wantedFields) {
        FieldTree rootFrame = FieldTree.createRoot();
        for (FieldSelector wantedField : wantedFields) {
            rootFrame.addEntry(wantedField);
        }
        this.stack.push(rootFrame);
    }

    @Override
    public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String id) {
        FieldTree newFrame;
        FieldTree currentFrame = this.stack.element();
        if (currentFrame.isSelected(type, id)) {
            return StreamTagVisitor.EntryResult.SKIP;
        }
        if (type == CompoundTag.TYPE && (newFrame = currentFrame.fieldsToRecurse().get(id)) != null) {
            this.stack.push(newFrame);
        }
        return super.visitEntry(type, id);
    }

    @Override
    public StreamTagVisitor.ValueResult visitContainerEnd() {
        if (this.depth() == this.stack.element().depth()) {
            this.stack.pop();
        }
        return super.visitContainerEnd();
    }
}

