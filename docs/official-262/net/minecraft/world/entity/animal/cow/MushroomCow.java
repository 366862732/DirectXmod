/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.animal.cow;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class MushroomCow
extends AbstractCow
implements Shearable {
    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.INT);
    private static final int MUTATE_CHANCE = 1024;
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(0.45f, 0.7f).withEyeHeight(0.69f).withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0f, 0.75f, 0.0f));
    private static final String TAG_STEW_EFFECTS = "stew_effects";
    private @Nullable SuspiciousStewEffects stewEffects;
    private @Nullable UUID lastLightningBoltUUID;

    public MushroomCow(EntityType<? extends MushroomCow> type, Level level) {
        super((EntityType<? extends AbstractCow>)type, level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos.below()).is(Blocks.MYCELIUM)) {
            return 10.0f;
        }
        return level.getPathfindingCostFromLightLevels(pos);
    }

    public static boolean checkMushroomSpawnRules(EntityType<MushroomCow> type, LevelAccessor level, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && MushroomCow.isBrightEnoughToSpawn(level, pos);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightningBolt) {
        UUID lightningBoltUUID = lightningBolt.getUUID();
        if (!lightningBoltUUID.equals(this.lastLightningBoltUUID)) {
            this.setVariant(this.getVariant() == Variant.RED ? Variant.BROWN : Variant.RED);
            this.lastLightningBoltUUID = lightningBoltUUID;
            this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0f, 1.0f);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_TYPE, Variant.DEFAULT.id);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.BOWL) && !this.isBaby()) {
            ItemStack stew;
            boolean isSuspicious = false;
            if (this.stewEffects != null) {
                isSuspicious = true;
                stew = new ItemStack(Items.SUSPICIOUS_STEW);
                stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, this.stewEffects);
                this.stewEffects = null;
            } else {
                stew = new ItemStack(Items.MUSHROOM_STEW);
            }
            ItemStack bowlOrStew = ItemUtils.createFilledResult(itemStack, player, stew, false);
            player.setItemInHand(hand, bowlOrStew);
            SoundEvent milkSound = isSuspicious ? SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY : SoundEvents.MOOSHROOM_MILK;
            this.playSound(milkSound, 1.0f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (itemStack.is(Items.SHEARS) && this.readyForShearing()) {
            Level isSuspicious = this.level();
            if (isSuspicious instanceof ServerLevel) {
                ServerLevel level = (ServerLevel)isSuspicious;
                this.shear(level, SoundSource.PLAYERS, itemStack);
                this.gameEvent(GameEvent.SHEAR, player);
                itemStack.hurtAndBreak(1, (LivingEntity)player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        if (this.getVariant() == Variant.BROWN && !this.isBaby()) {
            Optional<SuspiciousStewEffects> effectsFromItemStack = this.getEffectsFromItemStack(itemStack);
            if (effectsFromItemStack.isEmpty()) {
                return super.mobInteract(player, hand);
            }
            if (this.stewEffects != null) {
                for (int i = 0; i < 2; ++i) {
                    this.level().addParticle(ParticleTypes.SMOKE, this.getX() + this.random.nextDouble() / 2.0, this.getY(0.5), this.getZ() + this.random.nextDouble() / 2.0, 0.0, this.random.nextDouble() / 5.0, 0.0);
                }
            } else {
                itemStack.consume(1, player);
                SpellParticleOption particle = SpellParticleOption.create(ParticleTypes.EFFECT, -1, 1.0f);
                for (int i = 0; i < 4; ++i) {
                    this.level().addParticle(particle, this.getX() + this.random.nextDouble() / 2.0, this.getY(0.5), this.getZ() + this.random.nextDouble() / 2.0, 0.0, this.random.nextDouble() / 5.0, 0.0);
                }
                this.stewEffects = effectsFromItemStack.get();
                this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void shear(ServerLevel level, SoundSource soundSource, ItemStack tool) {
        level.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, soundSource, 1.0f, 1.0f);
        this.convertTo(EntityTypes.COW, ConversionParams.single(this, false, false), cow -> {
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_MOOSHROOM, tool, (l, drop) -> {
                for (int i = 0; i < drop.getCount(); ++i) {
                    l.addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0), this.getZ(), drop.copyWithCount(1)));
                }
            });
        });
    }

    @Override
    public boolean readyForShearing() {
        return !this.isBaby();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Type", Variant.CODEC, this.getVariant());
        output.storeNullable(TAG_STEW_EFFECTS, SuspiciousStewEffects.CODEC, this.stewEffects);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.read("Type", Variant.CODEC).orElse(Variant.DEFAULT));
        this.stewEffects = input.read(TAG_STEW_EFFECTS, SuspiciousStewEffects.CODEC).orElse(null);
    }

    private Optional<SuspiciousStewEffects> getEffectsFromItemStack(ItemStack itemStack) {
        SuspiciousEffectHolder effectHolder = SuspiciousEffectHolder.tryGet(itemStack.getItem());
        if (effectHolder != null) {
            return Optional.of(effectHolder.getSuspiciousEffects());
        }
        return Optional.empty();
    }

    private void setVariant(Variant variant) {
        this.entityData.set(DATA_TYPE, variant.id);
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_TYPE));
    }

    @Override
    public <T> @Nullable T get(DataComponentType<? extends T> type) {
        if (type == DataComponents.MOOSHROOM_VARIANT) {
            return MushroomCow.castComponentValue(type, this.getVariant());
        }
        return super.get(type);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        this.applyImplicitComponentIfPresent(components, DataComponents.MOOSHROOM_VARIANT);
        super.applyImplicitComponents(components);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> type, T value) {
        if (type == DataComponents.MOOSHROOM_VARIANT) {
            this.setVariant(MushroomCow.castComponentValue(DataComponents.MOOSHROOM_VARIANT, value));
            return true;
        }
        return super.applyImplicitComponent(type, value);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    public @Nullable MushroomCow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        MushroomCow baby = EntityTypes.MOOSHROOM.create((Level)level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setVariant(this.getOffspringVariant((MushroomCow)partner));
        }
        return baby;
    }

    private Variant getOffspringVariant(MushroomCow mate) {
        Variant mateVariant;
        Variant variant = this.getVariant();
        Variant babyVariant = variant == (mateVariant = mate.getVariant()) && this.random.nextInt(1024) == 0 ? (variant == Variant.BROWN ? Variant.RED : Variant.BROWN) : (this.random.nextBoolean() ? variant : mateVariant);
        return babyVariant;
    }

    public static enum Variant implements StringRepresentable
    {
        RED("red", 0, Blocks.RED_MUSHROOM.defaultBlockState()),
        BROWN("brown", 1, Blocks.BROWN_MUSHROOM.defaultBlockState());

        public static final Variant DEFAULT;
        public static final Codec<Variant> CODEC;
        private static final IntFunction<Variant> BY_ID;
        public static final StreamCodec<ByteBuf, Variant> STREAM_CODEC;
        private final String type;
        private final int id;
        private final BlockState blockState;

        private Variant(String type, int id, BlockState blockState) {
            this.type = type;
            this.id = id;
            this.blockState = blockState;
        }

        public BlockState getBlockState() {
            return this.blockState;
        }

        @Override
        public String getSerializedName() {
            return this.type;
        }

        private int id() {
            return this.id;
        }

        private static Variant byId(int id) {
            return BY_ID.apply(id);
        }

        static {
            DEFAULT = RED;
            CODEC = StringRepresentable.fromEnum(Variant::values);
            BY_ID = ByIdMap.continuous(Variant::id, Variant.values(), ByIdMap.OutOfBoundsStrategy.CLAMP);
            STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Variant::id);
        }
    }
}

