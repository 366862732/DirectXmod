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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityTagPredicate(Optional<List<String>> anyOf, Optional<List<String>> allOf, Optional<List<String>> noneOf) implements EntitySubPredicate
{
    public static final Codec<EntityTagPredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.listOf().optionalFieldOf("any_of").forGetter(EntityTagPredicate::anyOf), (App)Codec.STRING.listOf().optionalFieldOf("all_of").forGetter(EntityTagPredicate::allOf), (App)Codec.STRING.listOf().optionalFieldOf("none_of").forGetter(EntityTagPredicate::noneOf)).apply((Applicative)i, EntityTagPredicate::new));

    public boolean matches(Set<String> tags) {
        if (this.anyOf.isPresent() && !EntityTagPredicate.containsAtLeastOne(tags, this.anyOf.get())) {
            return false;
        }
        if (this.noneOf.isPresent() && EntityTagPredicate.containsAtLeastOne(tags, this.noneOf.get())) {
            return false;
        }
        return !this.allOf.isPresent() || EntityTagPredicate.containsAllOf(tags, this.allOf.get());
    }

    @Override
    public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
        return this.matches(entity.entityTags());
    }

    private static boolean containsAtLeastOne(Set<String> provided, List<String> tags) {
        for (String tag : tags) {
            if (!provided.contains(tag)) continue;
            return true;
        }
        return false;
    }

    private static boolean containsAllOf(Set<String> provided, List<String> tags) {
        for (String tag : tags) {
            if (provided.contains(tag)) continue;
            return false;
        }
        return true;
    }
}

