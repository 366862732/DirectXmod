/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity, Optional<Boolean> isDirect) {
    public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)TagPredicate.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("tags", List.of()).forGetter(DamageSourcePredicate::tags), (App)EntityPredicate.CODEC.optionalFieldOf("direct_entity").forGetter(DamageSourcePredicate::directEntity), (App)EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamageSourcePredicate::sourceEntity), (App)Codec.BOOL.optionalFieldOf("is_direct").forGetter(DamageSourcePredicate::isDirect)).apply((Applicative)i, DamageSourcePredicate::new));

    public boolean matches(ServerPlayer player, DamageSource source) {
        return this.matches(player.level(), player.position(), source);
    }

    public boolean matches(ServerLevel level, Vec3 position, DamageSource source) {
        for (TagPredicate<DamageType> tag : this.tags) {
            if (tag.matches(source.typeHolder())) continue;
            return false;
        }
        if (this.directEntity.isPresent() && !this.directEntity.get().matches(level, position, source.getDirectEntity())) {
            return false;
        }
        if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(level, position, source.getEntity())) {
            return false;
        }
        return !this.isDirect.isPresent() || this.isDirect.get().booleanValue() == source.isDirect();
    }

    public static class Builder {
        private final ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
        private Optional<EntityPredicate> directEntity = Optional.empty();
        private Optional<EntityPredicate> sourceEntity = Optional.empty();
        private Optional<Boolean> isDirect = Optional.empty();

        public static Builder damageType() {
            return new Builder();
        }

        public Builder tag(TagPredicate<DamageType> tag) {
            this.tags.add(tag);
            return this;
        }

        public Builder direct(EntityPredicate.Builder directEntity) {
            this.directEntity = Optional.of(directEntity.build());
            return this;
        }

        public Builder source(EntityPredicate.Builder sourceEntity) {
            this.sourceEntity = Optional.of(sourceEntity.build());
            return this;
        }

        public Builder isDirect(boolean direct) {
            this.isDirect = Optional.of(direct);
            return this;
        }

        public DamageSourcePredicate build() {
            return new DamageSourcePredicate((List<TagPredicate<DamageType>>)this.tags.build(), this.directEntity, this.sourceEntity, this.isDirect);
        }
    }
}

