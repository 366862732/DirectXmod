/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextUser;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetStewEffectFunction
extends LootItemConditionalFunction {
    private static final Codec<List<EffectEntry>> EFFECTS_LIST = EffectEntry.CODEC.listOf().validate(entries -> {
        ObjectOpenHashSet seenEffects = new ObjectOpenHashSet();
        for (EffectEntry entry : entries) {
            if (seenEffects.add(entry.effect())) continue;
            return DataResult.error(() -> "Encountered duplicate mob effect: '" + String.valueOf(entry.effect()) + "'");
        }
        return DataResult.success((Object)entries);
    });
    public static final MapCodec<SetStewEffectFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetStewEffectFunction.commonFields(i).and((App)EFFECTS_LIST.optionalFieldOf("effects", List.of()).forGetter(f -> f.effects)).apply((Applicative)i, SetStewEffectFunction::new));
    private final List<EffectEntry> effects;

    private SetStewEffectFunction(List<LootItemCondition> predicates, List<EffectEntry> effects) {
        super(predicates);
        this.effects = effects;
    }

    public MapCodec<SetStewEffectFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "effects", this.effects);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        if (!itemStack.is(Items.SUSPICIOUS_STEW) || this.effects.isEmpty()) {
            return itemStack;
        }
        EffectEntry entry = Util.getRandom(this.effects, context.getRandom());
        Holder<MobEffect> effect = entry.effect();
        int duration = entry.duration().getInt(context);
        if (!effect.value().isInstantaneous()) {
            duration *= 20;
        }
        SuspiciousStewEffects.Entry newEntry = new SuspiciousStewEffects.Entry(effect, duration);
        itemStack.update(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY, newEntry, SuspiciousStewEffects::withEffectAdded);
        return itemStack;
    }

    public static Builder stewEffect() {
        return new Builder();
    }

    private record EffectEntry(Holder<MobEffect> effect, NumberProvider duration) implements LootContextUser
    {
        public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)MobEffect.CODEC.fieldOf("type").forGetter(EffectEntry::effect), (App)NumberProviders.CODEC.fieldOf("duration").forGetter(EffectEntry::duration)).apply((Applicative)i, EffectEntry::new));

        @Override
        public void validate(ValidationContext context) {
            LootContextUser.super.validate(context);
            Validatable.validate(context, "duration", this.duration);
        }
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final ImmutableList.Builder<EffectEntry> effects = ImmutableList.builder();

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder withEffect(Holder<MobEffect> effect, NumberProvider duration) {
            this.effects.add((Object)new EffectEntry(effect, duration));
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetStewEffectFunction(this.getConditions(), (List<EffectEntry>)this.effects.build());
        }
    }
}

