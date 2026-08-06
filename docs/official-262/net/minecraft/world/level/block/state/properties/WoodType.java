/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 */
package net.minecraft.world.level.block.state.properties;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public record WoodType(String name, BlockSetType setType, SoundType soundType, SoundType hangingSignSoundType, SoundEvent fenceGateClose, SoundEvent fenceGateOpen) {
    private static final Map<String, WoodType> TYPES = new Object2ObjectArrayMap();
    public static final Codec<WoodType> CODEC = Codec.stringResolver(WoodType::name, TYPES::get);
    public static final WoodType OAK = WoodType.register(new WoodType("oak", BlockSetType.OAK));
    public static final WoodType SPRUCE = WoodType.register(new WoodType("spruce", BlockSetType.SPRUCE));
    public static final WoodType BIRCH = WoodType.register(new WoodType("birch", BlockSetType.BIRCH));
    public static final WoodType ACACIA = WoodType.register(new WoodType("acacia", BlockSetType.ACACIA));
    public static final WoodType CHERRY = WoodType.register(new WoodType("cherry", BlockSetType.CHERRY, SoundType.CHERRY_WOOD, SoundType.CHERRY_WOOD_HANGING_SIGN, SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN));
    public static final WoodType JUNGLE = WoodType.register(new WoodType("jungle", BlockSetType.JUNGLE));
    public static final WoodType DARK_OAK = WoodType.register(new WoodType("dark_oak", BlockSetType.DARK_OAK));
    public static final WoodType PALE_OAK = WoodType.register(new WoodType("pale_oak", BlockSetType.PALE_OAK));
    public static final WoodType CRIMSON = WoodType.register(new WoodType("crimson", BlockSetType.CRIMSON, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN));
    public static final WoodType WARPED = WoodType.register(new WoodType("warped", BlockSetType.WARPED, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN));
    public static final WoodType MANGROVE = WoodType.register(new WoodType("mangrove", BlockSetType.MANGROVE));
    public static final WoodType BAMBOO = WoodType.register(new WoodType("bamboo", BlockSetType.BAMBOO, SoundType.BAMBOO_WOOD, SoundType.BAMBOO_WOOD_HANGING_SIGN, SoundEvents.BAMBOO_WOOD_FENCE_GATE_CLOSE, SoundEvents.BAMBOO_WOOD_FENCE_GATE_OPEN));

    public WoodType(String name, BlockSetType setType) {
        this(name, setType, SoundType.WOOD, SoundType.HANGING_SIGN, SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);
    }

    private static WoodType register(WoodType type) {
        TYPES.put(type.name(), type);
        return type;
    }

    public static Stream<WoodType> values() {
        return TYPES.values().stream();
    }
}

