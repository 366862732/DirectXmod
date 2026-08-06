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
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record EnchantmentActiveCheck(boolean active) implements LootItemCondition
{
    public static final MapCodec<EnchantmentActiveCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.fieldOf("active").forGetter(EnchantmentActiveCheck::active)).apply((Applicative)i, EnchantmentActiveCheck::new));

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getParameter(LootContextParams.ENCHANTMENT_ACTIVE) == this.active;
    }

    public MapCodec<EnchantmentActiveCheck> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ENCHANTMENT_ACTIVE);
    }

    public static LootItemCondition.Builder enchantmentActiveCheck() {
        return () -> new EnchantmentActiveCheck(true);
    }

    public static LootItemCondition.Builder enchantmentInactiveCheck() {
        return () -> new EnchantmentActiveCheck(false);
    }
}

