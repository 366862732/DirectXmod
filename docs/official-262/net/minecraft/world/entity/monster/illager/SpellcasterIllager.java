/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.monster.illager;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.IntFunction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public abstract class SpellcasterIllager
extends AbstractIllager {
    private static final EntityDataAccessor<Byte> DATA_SPELL_CASTING_ID = SynchedEntityData.defineId(SpellcasterIllager.class, EntityDataSerializers.BYTE);
    private static final int DEFAULT_SPELLCASTING_TICKS = 0;
    protected int spellCastingTickCount = 0;
    private IllagerSpell currentSpell = IllagerSpell.NONE;

    protected SpellcasterIllager(EntityType<? extends SpellcasterIllager> type, Level level) {
        super((EntityType<? extends AbstractIllager>)type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_SPELL_CASTING_ID, (byte)0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.spellCastingTickCount = input.getIntOr("SpellTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("SpellTicks", this.spellCastingTickCount);
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.isCastingSpell()) {
            return AbstractIllager.IllagerArmPose.SPELLCASTING;
        }
        if (this.isCelebrating()) {
            return AbstractIllager.IllagerArmPose.CELEBRATING;
        }
        return AbstractIllager.IllagerArmPose.CROSSED;
    }

    public boolean isCastingSpell() {
        if (this.level().isClientSide()) {
            return this.entityData.get(DATA_SPELL_CASTING_ID) > 0;
        }
        return this.spellCastingTickCount > 0;
    }

    public void setIsCastingSpell(IllagerSpell spell) {
        this.currentSpell = spell;
        this.entityData.set(DATA_SPELL_CASTING_ID, (byte)spell.id);
    }

    protected IllagerSpell getCurrentSpell() {
        if (!this.level().isClientSide()) {
            return this.currentSpell;
        }
        return IllagerSpell.byId(this.entityData.get(DATA_SPELL_CASTING_ID).byteValue());
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.spellCastingTickCount > 0) {
            --this.spellCastingTickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.isCastingSpell()) {
            IllagerSpell spell = this.getCurrentSpell();
            float red = (float)spell.spellColor[0];
            float green = (float)spell.spellColor[1];
            float blue = (float)spell.spellColor[2];
            float bodyAngle = this.yBodyRot * ((float)Math.PI / 180) + Mth.cos((float)this.tickCount * 0.6662f) * 0.25f;
            float cos = Mth.cos(bodyAngle);
            float sin = Mth.sin(bodyAngle);
            double handDistance = 0.6 * (double)this.getScale();
            double handHeight = 1.8 * (double)this.getScale();
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, red, green, blue), this.getX() + (double)cos * handDistance, this.getY() + handHeight, this.getZ() + (double)sin * handDistance, 0.0, 0.0, 0.0);
            this.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, red, green, blue), this.getX() - (double)cos * handDistance, this.getY() + handHeight, this.getZ() - (double)sin * handDistance, 0.0, 0.0, 0.0);
        }
    }

    protected int getSpellCastingTime() {
        return this.spellCastingTickCount;
    }

    protected abstract SoundEvent getCastingSoundEvent();

    protected static enum IllagerSpell {
        NONE(0, 0.0, 0.0, 0.0),
        SUMMON_VEX(1, 0.7, 0.7, 0.8),
        FANGS(2, 0.4, 0.3, 0.35),
        WOLOLO(3, 0.7, 0.5, 0.2),
        DISAPPEAR(4, 0.3, 0.3, 0.8),
        BLINDNESS(5, 0.1, 0.1, 0.2);

        private static final IntFunction<IllagerSpell> BY_ID;
        private final int id;
        private final double[] spellColor;

        private IllagerSpell(int id, double red, double green, double blue) {
            this.id = id;
            this.spellColor = new double[]{red, green, blue};
        }

        public static IllagerSpell byId(int id) {
            return BY_ID.apply(id);
        }

        static {
            BY_ID = ByIdMap.continuous(e -> e.id, IllagerSpell.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        }
    }

    protected abstract class SpellcasterUseSpellGoal
    extends Goal {
        protected int attackWarmupDelay;
        protected int nextAttackTickCount;
        final /* synthetic */ SpellcasterIllager this$0;

        protected SpellcasterUseSpellGoal(SpellcasterIllager this$0) {
            SpellcasterIllager spellcasterIllager = this$0;
            Objects.requireNonNull(spellcasterIllager);
            this.this$0 = spellcasterIllager;
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.this$0.getTarget();
            if (target == null || !target.isAlive()) {
                return false;
            }
            if (this.this$0.isCastingSpell()) {
                return false;
            }
            return this.this$0.tickCount >= this.nextAttackTickCount;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.this$0.getTarget();
            return target != null && target.isAlive() && this.attackWarmupDelay > 0;
        }

        @Override
        public void start() {
            this.attackWarmupDelay = this.adjustedTickDelay(this.getCastWarmupTime());
            this.this$0.spellCastingTickCount = this.getCastingTime();
            this.nextAttackTickCount = this.this$0.tickCount + this.getCastingInterval();
            SoundEvent spellPrepareSound = this.getSpellPrepareSound();
            if (spellPrepareSound != null) {
                this.this$0.playSound(spellPrepareSound, 1.0f, 1.0f);
            }
            this.this$0.setIsCastingSpell(this.getSpell());
        }

        @Override
        public void tick() {
            --this.attackWarmupDelay;
            if (this.attackWarmupDelay == 0) {
                this.performSpellCasting();
                this.this$0.playSound(this.this$0.getCastingSoundEvent(), 1.0f, 1.0f);
            }
        }

        protected abstract void performSpellCasting();

        protected int getCastWarmupTime() {
            return 20;
        }

        protected abstract int getCastingTime();

        protected abstract int getCastingInterval();

        protected abstract @Nullable SoundEvent getSpellPrepareSound();

        protected abstract IllagerSpell getSpell();
    }

    protected class SpellcasterCastingSpellGoal
    extends Goal {
        final /* synthetic */ SpellcasterIllager this$0;

        public SpellcasterCastingSpellGoal(SpellcasterIllager this$0) {
            SpellcasterIllager spellcasterIllager = this$0;
            Objects.requireNonNull(spellcasterIllager);
            this.this$0 = spellcasterIllager;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.this$0.getSpellCastingTime() > 0;
        }

        @Override
        public void start() {
            super.start();
            this.this$0.navigation.stop();
        }

        @Override
        public void stop() {
            super.stop();
            this.this$0.setIsCastingSpell(IllagerSpell.NONE);
        }

        @Override
        public void tick() {
            if (this.this$0.getTarget() != null) {
                this.this$0.getLookControl().setLookAt(this.this$0.getTarget(), this.this$0.getMaxHeadYRot(), this.this$0.getMaxHeadXRot());
            }
        }
    }
}

