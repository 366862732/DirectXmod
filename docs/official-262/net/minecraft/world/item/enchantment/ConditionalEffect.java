/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record ConditionalEffect<T>(T effect, Optional<LootItemCondition> requirements) implements Validatable
{
    public static <T> Codec<ConditionalEffect<T>> codec(Codec<T> effectCodec) {
        return RecordCodecBuilder.create(i -> i.group((App)effectCodec.fieldOf("effect").forGetter(ConditionalEffect::effect), (App)LootItemCondition.DIRECT_CODEC.optionalFieldOf("requirements").forGetter(ConditionalEffect::requirements)).apply((Applicative)i, ConditionalEffect::new));
    }

    public boolean matches(LootContext context) {
        return this.requirements.isEmpty() || this.requirements.get().test(context);
    }

    @Override
    public void validate(ValidationContext context) {
        Validatable.validate(context, "requirements", this.requirements);
    }
}

