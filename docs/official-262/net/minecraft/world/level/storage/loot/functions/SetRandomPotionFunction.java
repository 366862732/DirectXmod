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
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetRandomPotionFunction
extends LootItemConditionalFunction {
    public static final MapCodec<SetRandomPotionFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> SetRandomPotionFunction.commonFields(i).and((App)RegistryCodecs.homogeneousList(Registries.POTION).optionalFieldOf("options").forGetter(f -> f.options)).apply((Applicative)i, SetRandomPotionFunction::new));
    private final Optional<HolderSet<Potion>> options;

    private SetRandomPotionFunction(List<LootItemCondition> predicates, Optional<HolderSet<Potion>> options) {
        super(predicates);
        this.options = options;
    }

    public MapCodec<SetRandomPotionFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Optional<Holder<Object>> potion = this.options.isPresent() ? this.options.get().getRandomElement(context.getRandom()) : context.getLevel().registryAccess().lookupOrThrow(Registries.POTION).getRandom(context.getRandom());
        if (potion.isPresent()) {
            itemStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, potion.get(), PotionContents::withPotion);
        }
        return itemStack;
    }

    public static LootItemConditionalFunction.Builder<?> fromTagKey(HolderSet<Potion> tagKey) {
        return SetRandomPotionFunction.simpleBuilder(conditions -> new SetRandomPotionFunction((List<LootItemCondition>)conditions, Optional.of(tagKey)));
    }
}

