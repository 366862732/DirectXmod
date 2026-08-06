/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

public enum EquipmentSlotGroup implements StringRepresentable,
Iterable<EquipmentSlot>
{
    ANY(0, "any", slot -> true),
    MAINHAND(1, "mainhand", EquipmentSlot.MAINHAND),
    OFFHAND(2, "offhand", EquipmentSlot.OFFHAND),
    HAND(3, "hand", slot -> slot.getType() == EquipmentSlot.Type.HAND),
    FEET(4, "feet", EquipmentSlot.FEET),
    LEGS(5, "legs", EquipmentSlot.LEGS),
    CHEST(6, "chest", EquipmentSlot.CHEST),
    HEAD(7, "head", EquipmentSlot.HEAD),
    ARMOR(8, "armor", EquipmentSlot::isArmor),
    BODY(9, "body", EquipmentSlot.BODY),
    SADDLE(10, "saddle", EquipmentSlot.SADDLE);

    public static final IntFunction<EquipmentSlotGroup> BY_ID;
    public static final Codec<EquipmentSlotGroup> CODEC;
    public static final StreamCodec<ByteBuf, EquipmentSlotGroup> STREAM_CODEC;
    private final int id;
    private final String key;
    private final Predicate<EquipmentSlot> predicate;
    private final List<EquipmentSlot> slots;

    private EquipmentSlotGroup(int id, String key, Predicate<EquipmentSlot> predicate) {
        this.id = id;
        this.key = key;
        this.predicate = predicate;
        this.slots = EquipmentSlot.VALUES.stream().filter(predicate).toList();
    }

    private EquipmentSlotGroup(int id, String key, EquipmentSlot slot) {
        this(id, key, (EquipmentSlot s) -> s == slot);
    }

    public static EquipmentSlotGroup bySlot(EquipmentSlot slot) {
        return switch (slot) {
            default -> throw new MatchException(null, null);
            case EquipmentSlot.MAINHAND -> MAINHAND;
            case EquipmentSlot.OFFHAND -> OFFHAND;
            case EquipmentSlot.FEET -> FEET;
            case EquipmentSlot.LEGS -> LEGS;
            case EquipmentSlot.CHEST -> CHEST;
            case EquipmentSlot.HEAD -> HEAD;
            case EquipmentSlot.BODY -> BODY;
            case EquipmentSlot.SADDLE -> SADDLE;
        };
    }

    @Override
    public String getSerializedName() {
        return this.key;
    }

    public boolean test(EquipmentSlot slot) {
        return this.predicate.test(slot);
    }

    public List<EquipmentSlot> slots() {
        return this.slots;
    }

    @Override
    public Iterator<EquipmentSlot> iterator() {
        return this.slots.iterator();
    }

    static {
        BY_ID = ByIdMap.continuous(s -> s.id, EquipmentSlotGroup.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        CODEC = StringRepresentable.fromEnum(EquipmentSlotGroup::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, s -> s.id);
    }
}

