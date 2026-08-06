/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.ints.IntLists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.inventory;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.SlotRange;
import org.jspecify.annotations.Nullable;

public class SlotRanges {
    public static final int MOB_INVENTORY_SLOT_OFFSET = 300;
    public static final int MOB_INVENTORY_SIZE = 8;
    private static final List<SlotRange> SLOTS = Util.make(new ArrayList(), values -> {
        SlotRanges.addSingleSlot(values, "contents", 0);
        SlotRanges.addSlotRange(values, "container.", 0, 54);
        SlotRanges.addSlotRange(values, "hotbar.", 0, 9);
        SlotRanges.addSlotRange(values, "inventory.", 9, 27);
        SlotRanges.addSlotRange(values, "enderchest.", 200, 27);
        SlotRanges.addSlotRange(values, "mob.inventory.", 300, 8);
        SlotRanges.addSlotRange(values, "horse.", 500, 15);
        int mainhand = EquipmentSlot.MAINHAND.getIndex(98);
        int offhand = EquipmentSlot.OFFHAND.getIndex(98);
        SlotRanges.addSingleSlot(values, "weapon", mainhand);
        SlotRanges.addSingleSlot(values, "weapon.mainhand", mainhand);
        SlotRanges.addSingleSlot(values, "weapon.offhand", offhand);
        SlotRanges.addSlots(values, "weapon.*", mainhand, offhand);
        int head = EquipmentSlot.HEAD.getIndex(100);
        int chest = EquipmentSlot.CHEST.getIndex(100);
        int legs = EquipmentSlot.LEGS.getIndex(100);
        int feet = EquipmentSlot.FEET.getIndex(100);
        int body = EquipmentSlot.BODY.getIndex(105);
        SlotRanges.addSingleSlot(values, "armor.head", head);
        SlotRanges.addSingleSlot(values, "armor.chest", chest);
        SlotRanges.addSingleSlot(values, "armor.legs", legs);
        SlotRanges.addSingleSlot(values, "armor.feet", feet);
        SlotRanges.addSingleSlot(values, "armor.body", body);
        SlotRanges.addSlots(values, "armor.*", head, chest, legs, feet, body);
        SlotRanges.addSingleSlot(values, "saddle", EquipmentSlot.SADDLE.getIndex(106));
        SlotRanges.addSingleSlot(values, "horse.chest", 499);
        SlotRanges.addSingleSlot(values, "player.cursor", 499);
        SlotRanges.addSlotRange(values, "player.crafting.", 500, 4);
    });
    public static final Codec<SlotRange> CODEC = StringRepresentable.fromValues(() -> (SlotRange[])SLOTS.toArray(SlotRange[]::new));
    private static final Function<String, @Nullable SlotRange> NAME_LOOKUP = StringRepresentable.createNameLookup((StringRepresentable[])((SlotRange[])SLOTS.toArray(SlotRange[]::new)));

    private static SlotRange create(String name, int id) {
        return SlotRange.of(name, IntLists.singleton((int)id));
    }

    private static SlotRange create(String name, IntList ids) {
        return SlotRange.of(name, IntLists.unmodifiable((IntList)ids));
    }

    private static SlotRange create(String name, int ... ids) {
        return SlotRange.of(name, IntList.of((int[])ids));
    }

    private static void addSingleSlot(List<SlotRange> output, String name, int id) {
        output.add(SlotRanges.create(name, id));
    }

    private static void addSlotRange(List<SlotRange> output, String prefix, int offset, int size) {
        IntArrayList allSlots = new IntArrayList(size);
        for (int i = 0; i < size; ++i) {
            int slotId = offset + i;
            output.add(SlotRanges.create(prefix + i, slotId));
            allSlots.add(slotId);
        }
        output.add(SlotRanges.create(prefix + "*", (IntList)allSlots));
    }

    private static void addSlots(List<SlotRange> output, String name, int ... values) {
        output.add(SlotRanges.create(name, values));
    }

    public static @Nullable SlotRange nameToIds(String name) {
        return NAME_LOOKUP.apply(name);
    }

    public static Stream<String> allNames() {
        return SLOTS.stream().map(StringRepresentable::getSerializedName);
    }

    public static Stream<String> singleSlotNames() {
        return SLOTS.stream().filter(e -> e.size() == 1).map(StringRepresentable::getSerializedName);
    }
}

