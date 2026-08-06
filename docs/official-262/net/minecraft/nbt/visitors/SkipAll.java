/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt.visitors;

import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.TagType;

public interface SkipAll
extends StreamTagVisitor {
    public static final SkipAll INSTANCE = new SkipAll(){};

    @Override
    default public StreamTagVisitor.ValueResult visitEnd() {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(String value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(byte value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(short value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(int value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(long value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(float value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(double value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(byte[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(int[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visit(long[] value) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visitList(TagType<?> elementType, int size) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.EntryResult visitElement(TagType<?> type, int index) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default public StreamTagVisitor.EntryResult visitEntry(TagType<?> type) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default public StreamTagVisitor.EntryResult visitEntry(TagType<?> type, String id) {
        return StreamTagVisitor.EntryResult.SKIP;
    }

    @Override
    default public StreamTagVisitor.ValueResult visitContainerEnd() {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }

    @Override
    default public StreamTagVisitor.ValueResult visitRootEntry(TagType<?> type) {
        return StreamTagVisitor.ValueResult.CONTINUE;
    }
}

