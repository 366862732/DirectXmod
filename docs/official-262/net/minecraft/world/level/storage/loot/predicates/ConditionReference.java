/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public record ConditionReference(ResourceKey<LootItemCondition> name) implements LootItemCondition
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<ConditionReference> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ResourceKey.codec(Registries.PREDICATE).fieldOf("name").forGetter(ConditionReference::name)).apply((Applicative)i, ConditionReference::new));

    public MapCodec<ConditionReference> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        LootItemCondition.super.validate(context);
        Validatable.validateReference(context, this.name);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean test(LootContext lootContext) {
        LootItemCondition condition = lootContext.getResolver().get(this.name).map(Holder.Reference::value).orElse(null);
        if (condition == null) {
            LOGGER.warn("Tried using unknown condition table called {}", (Object)this.name.identifier());
            return false;
        }
        LootContext.VisitedEntry<LootItemCondition> breadcrumb = LootContext.createVisitedEntry(condition);
        if (lootContext.pushVisitedElement(breadcrumb)) {
            try {
                boolean bl = condition.test(lootContext);
                return bl;
            }
            finally {
                lootContext.popVisitedElement(breadcrumb);
            }
        }
        LOGGER.warn("Detected infinite loop in loot tables");
        return false;
    }

    public static LootItemCondition.Builder conditionReference(ResourceKey<LootItemCondition> name) {
        return () -> new ConditionReference(name);
    }
}

