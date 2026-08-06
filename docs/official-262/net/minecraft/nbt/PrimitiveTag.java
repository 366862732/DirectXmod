/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.nbt;

import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public sealed interface PrimitiveTag
extends Tag
permits NumericTag, StringTag {
    @Override
    default public Tag copy() {
        return this;
    }
}

