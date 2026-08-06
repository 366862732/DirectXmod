/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements.predicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(MinMaxBounds.Doubles dealtDamage, MinMaxBounds.Doubles takenDamage, Optional<EntityPredicate> sourceEntity, Optional<Boolean> blocked, Optional<DamageSourcePredicate> type) {
    public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(i -> i.group((App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("dealt", (Object)MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage), (App)MinMaxBounds.Doubles.CODEC.optionalFieldOf("taken", (Object)MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage), (App)EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamagePredicate::sourceEntity), (App)Codec.BOOL.optionalFieldOf("blocked").forGetter(DamagePredicate::blocked), (App)DamageSourcePredicate.CODEC.optionalFieldOf("type").forGetter(DamagePredicate::type)).apply((Applicative)i, DamagePredicate::new));

    public boolean matches(ServerPlayer player, DamageSource source, float originalDamage, float actualDamage, boolean blocked) {
        if (!this.dealtDamage.matches(originalDamage)) {
            return false;
        }
        if (!this.takenDamage.matches(actualDamage)) {
            return false;
        }
        if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(player, source.getEntity())) {
            return false;
        }
        if (this.blocked.isPresent() && this.blocked.get() != blocked) {
            return false;
        }
        return !this.type.isPresent() || this.type.get().matches(player, source);
    }

    public static class Builder {
        private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
        private Optional<EntityPredicate> sourceEntity = Optional.empty();
        private Optional<Boolean> blocked = Optional.empty();
        private Optional<DamageSourcePredicate> type = Optional.empty();

        public static Builder damageInstance() {
            return new Builder();
        }

        public Builder dealtDamage(MinMaxBounds.Doubles dealtDamage) {
            this.dealtDamage = dealtDamage;
            return this;
        }

        public Builder takenDamage(MinMaxBounds.Doubles takenDamage) {
            this.takenDamage = takenDamage;
            return this;
        }

        public Builder sourceEntity(EntityPredicate sourceEntity) {
            this.sourceEntity = Optional.of(sourceEntity);
            return this;
        }

        public Builder blocked(Boolean blocked) {
            this.blocked = Optional.of(blocked);
            return this;
        }

        public Builder type(DamageSourcePredicate type) {
            this.type = Optional.of(type);
            return this;
        }

        public Builder type(DamageSourcePredicate.Builder type) {
            this.type = Optional.of(type.build());
            return this;
        }

        public DamagePredicate build() {
            return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
        }
    }
}

