/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetInstrumentFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetInstrumentFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetInstrumentFunction.commonFields(i).and((App)RegistryCodecs.homogeneousList(Registries.INSTRUMENT).fieldOf("options").forGetter(f -> f.options)).apply((Applicative)i, SetInstrumentFunction::new));
    private final HolderSet<Instrument> options;

    private SetInstrumentFunction(List<LootItemCondition> predicates, HolderSet<Instrument> options) {
        super(predicates);
        this.options = options;
    }

    public MapCodec<SetInstrumentFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        this.options.getRandomElement(context.getRandom()).ifPresent(instrumentHolder -> itemStack.set(DataComponents.INSTRUMENT, new InstrumentComponent((Holder<Instrument>)instrumentHolder)));
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> setInstrumentOptions(HolderSet<Instrument> options) {
        return SetInstrumentFunction.simpleBuilder(conditions -> new SetInstrumentFunction((List<LootItemCondition>)conditions, options));
    }
}

