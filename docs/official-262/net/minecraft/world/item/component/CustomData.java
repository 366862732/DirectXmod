/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC);
    public static final Codec<CustomData> CODEC = COMPOUND_TAG_CODEC.xmap(CustomData::new, data -> data.tag);
    @Deprecated
    public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, data -> data.tag);
    private final CompoundTag tag;

    private CustomData(CompoundTag tag) {
        this.tag = tag;
    }

    public static CustomData of(CompoundTag tag) {
        return new CustomData(tag.copy());
    }

    public boolean matchedBy(CompoundTag expectedTag) {
        return NbtUtils.compareNbt(expectedTag, this.tag, true);
    }

    public static void update(DataComponentType<CustomData> component, ItemStack itemStack, Consumer<CompoundTag> consumer) {
        CustomData newData = itemStack.getOrDefault(component, EMPTY).update(consumer);
        if (newData.tag.isEmpty()) {
            itemStack.remove(component);
        } else {
            itemStack.set(component, newData);
        }
    }

    public static void set(DataComponentType<CustomData> component, ItemStack itemStack, CompoundTag tag) {
        if (!tag.isEmpty()) {
            itemStack.set(component, CustomData.of(tag));
        } else {
            itemStack.remove(component);
        }
    }

    public CustomData update(Consumer<CompoundTag> consumer) {
        CompoundTag newTag = this.tag.copy();
        consumer.accept(newTag);
        return new CustomData(newTag);
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CustomData) {
            CustomData customData = (CustomData)obj;
            return this.tag.equals(customData.tag);
        }
        return false;
    }

    public int hashCode() {
        return this.tag.hashCode();
    }

    public String toString() {
        return this.tag.toString();
    }
}

