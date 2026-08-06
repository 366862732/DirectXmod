/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;

public class ItemParticleOption
implements ParticleOptions {
    private final ParticleType<ItemParticleOption> type;
    private final ItemStackTemplate itemStack;

    public static MapCodec<ItemParticleOption> codec(ParticleType<ItemParticleOption> type) {
        return ItemStackTemplate.CODEC.xmap(stack -> new ItemParticleOption(type, (ItemStackTemplate)stack), o -> o.itemStack).fieldOf("item");
    }

    public static StreamCodec<? super RegistryFriendlyByteBuf, ItemParticleOption> streamCodec(ParticleType<ItemParticleOption> type) {
        return ItemStackTemplate.STREAM_CODEC.map(stack -> new ItemParticleOption(type, (ItemStackTemplate)stack), o -> o.itemStack);
    }

    public ItemParticleOption(ParticleType<ItemParticleOption> type, Item item) {
        this(type, new ItemStackTemplate(item));
    }

    public ItemParticleOption(ParticleType<ItemParticleOption> type, ItemStackTemplate itemStack) {
        this.type = type;
        this.itemStack = itemStack;
    }

    public ParticleType<ItemParticleOption> getType() {
        return this.type;
    }

    public ItemStackTemplate getItem() {
        return this.itemStack;
    }
}

