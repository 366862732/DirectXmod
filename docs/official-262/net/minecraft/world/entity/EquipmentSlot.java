/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum EquipmentSlot implements StringRepresentable
{
    MAINHAND(Type.HAND, 0, 0, "mainhand"),
    OFFHAND(Type.HAND, 1, 5, "offhand"),
    FEET(Type.HUMANOID_ARMOR, 0, 1, 1, "feet"),
    LEGS(Type.HUMANOID_ARMOR, 1, 1, 2, "legs"),
    CHEST(Type.HUMANOID_ARMOR, 2, 1, 3, "chest"),
    HEAD(Type.HUMANOID_ARMOR, 3, 1, 4, "head"),
    BODY(Type.ANIMAL_ARMOR, 0, 1, 6, "body"),
    SADDLE(Type.SADDLE, 0, 1, 7, "saddle");

    public static final int NO_COUNT_LIMIT = 0;
    public static final List<EquipmentSlot> VALUES;
    public static final IntFunction<EquipmentSlot> BY_ID;
    public static final StringRepresentable.EnumCodec<EquipmentSlot> CODEC;
    public static final StreamCodec<ByteBuf, EquipmentSlot> STREAM_CODEC;
    private final Type type;
    private final int index;
    private final int countLimit;
    private final int id;
    private final String name;

    private EquipmentSlot(Type type, int index, int countLimit, int id, String name) {
        this.type = type;
        this.index = index;
        this.countLimit = countLimit;
        this.id = id;
        this.name = name;
    }

    private EquipmentSlot(Type type, int index, int filterFlag, String name) {
        this(type, index, 0, filterFlag, name);
    }

    public Type getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public int getIndex(int base) {
        return base + this.index;
    }

    public ItemStack limit(ItemStack toEquip) {
        return this.countLimit > 0 ? toEquip.split(this.countLimit) : toEquip;
    }

    public int getId() {
        return this.id;
    }

    public int getFilterBit(int offset) {
        return this.id + offset;
    }

    public String getName() {
        return this.name;
    }

    public boolean isArmor() {
        return this.type == Type.HUMANOID_ARMOR || this.type == Type.ANIMAL_ARMOR;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public boolean canIncreaseExperience() {
        return this.type != Type.SADDLE;
    }

    public static EquipmentSlot byName(String name) {
        EquipmentSlot slot = CODEC.byName(name);
        if (slot != null) {
            return slot;
        }
        throw new IllegalArgumentException("Invalid slot '" + name + "'");
    }

    static {
        VALUES = List.of(EquipmentSlot.values());
        BY_ID = ByIdMap.continuous(s -> s.id, EquipmentSlot.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, s -> s.id);
    }

    public static enum Type {
        HAND,
        HUMANOID_ARMOR,
        ANIMAL_ARMOR,
        SADDLE;

    }
}

