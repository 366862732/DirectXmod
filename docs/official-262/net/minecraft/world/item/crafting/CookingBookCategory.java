/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

public enum CookingBookCategory implements StringRepresentable
{
    FOOD(0, "food"),
    BLOCKS(1, "blocks"),
    MISC(2, "misc");

    private static final IntFunction<CookingBookCategory> BY_ID;
    public static final Codec<CookingBookCategory> CODEC;
    public static final StreamCodec<ByteBuf, CookingBookCategory> STREAM_CODEC;
    private final int id;
    private final String name;

    private CookingBookCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        BY_ID = ByIdMap.continuous(e -> e.id, CookingBookCategory.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        CODEC = StringRepresentable.fromEnum(CookingBookCategory::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, e -> e.id);
    }
}

