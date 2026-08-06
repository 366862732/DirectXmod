/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements.predicates.entity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record CubeMobPredicate(MinMaxBounds.Ints size) implements EntitySubPredicate
{
    public static final Codec<CubeMobPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Ints.CODEC.optionalFieldOf("size", (Object)MinMaxBounds.Ints.ANY).forGetter(CubeMobPredicate::size)).apply((Applicative)i, CubeMobPredicate::new));

    public static CubeMobPredicate sized(MinMaxBounds.Ints size) {
        return new CubeMobPredicate(size);
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        if (entity instanceof AbstractCubeMob) {
            AbstractCubeMob cubeMob = (AbstractCubeMob)entity;
            return this.size.matches(cubeMob.getSize());
        }
        return false;
    }
}

