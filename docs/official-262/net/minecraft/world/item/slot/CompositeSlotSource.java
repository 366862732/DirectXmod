/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.slot;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.item.slot.SlotCollection;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeSlotSource
implements SlotSource {
    protected final List<SlotSource> terms;
    private final Function<LootContext, SlotCollection> compositeSlotSource;

    protected CompositeSlotSource(List<SlotSource> terms) {
        this.terms = terms;
        this.compositeSlotSource = SlotSources.group(terms);
    }

    protected static <T extends CompositeSlotSource> MapCodec<T> createCodec(Function<List<SlotSource>, T> factory) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)SlotSources.CODEC.listOf().fieldOf("terms").forGetter(t -> t.terms)).apply((Applicative)i, factory));
    }

    protected static <T extends CompositeSlotSource> Codec<T> createInlineCodec(Function<List<SlotSource>, T> factory) {
        return SlotSources.CODEC.listOf().xmap(factory, t -> t.terms);
    }

    public abstract MapCodec<? extends CompositeSlotSource> codec();

    @Override
    public SlotCollection provide(LootContext context) {
        return this.compositeSlotSource.apply(context);
    }

    @Override
    public void validate(ValidationContext context) {
        SlotSource.super.validate(context);
        Validatable.validate(context, "terms", this.terms);
    }
}

