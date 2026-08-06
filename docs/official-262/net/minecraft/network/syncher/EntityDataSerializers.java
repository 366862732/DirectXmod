/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.network.syncher;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Rotations;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.chicken.ChickenSoundVariant;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.minecraft.world.entity.animal.cow.CowSoundVariant;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.minecraft.world.entity.animal.feline.CatSoundVariant;
import net.minecraft.world.entity.animal.feline.CatVariant;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.minecraft.world.entity.animal.pig.PigSoundVariant;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariant;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class EntityDataSerializers {
    private static final CrudeIncrementalIntIdentityHashBiMap<EntityDataSerializer<?>> SERIALIZERS = CrudeIncrementalIntIdentityHashBiMap.create(16);
    public static final EntityDataSerializer<Byte> BYTE = EntityDataSerializer.forValueType(ByteBufCodecs.BYTE);
    public static final EntityDataSerializer<Integer> INT = EntityDataSerializer.forValueType(ByteBufCodecs.VAR_INT);
    public static final EntityDataSerializer<Long> LONG = EntityDataSerializer.forValueType(ByteBufCodecs.VAR_LONG);
    public static final EntityDataSerializer<Float> FLOAT = EntityDataSerializer.forValueType(ByteBufCodecs.FLOAT);
    public static final EntityDataSerializer<String> STRING = EntityDataSerializer.forValueType(ByteBufCodecs.STRING_UTF8);
    public static final EntityDataSerializer<Component> COMPONENT = EntityDataSerializer.forValueType(ComponentSerialization.TRUSTED_STREAM_CODEC);
    public static final EntityDataSerializer<Optional<Component>> OPTIONAL_COMPONENT = EntityDataSerializer.forValueType(ComponentSerialization.TRUSTED_OPTIONAL_STREAM_CODEC);
    public static final EntityDataSerializer<ItemStack> ITEM_STACK = new EntityDataSerializer<ItemStack>(){

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, ItemStack> codec() {
            return ItemStack.OPTIONAL_STREAM_CODEC;
        }

        @Override
        public ItemStack copy(ItemStack value) {
            return value.copy();
        }
    };
    public static final EntityDataSerializer<BlockState> BLOCK_STATE = EntityDataSerializer.forValueType(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY));
    private static final StreamCodec<ByteBuf, Optional<BlockState>> OPTIONAL_BLOCK_STATE_CODEC = new StreamCodec<ByteBuf, Optional<BlockState>>(){

        @Override
        public void encode(ByteBuf output, Optional<BlockState> value) {
            if (value.isPresent()) {
                VarInt.write(output, Block.getId(value.get()));
            } else {
                VarInt.write(output, 0);
            }
        }

        @Override
        public Optional<BlockState> decode(ByteBuf input) {
            int id = VarInt.read(input);
            if (id == 0) {
                return Optional.empty();
            }
            return Optional.of(Block.stateById(id));
        }
    };
    public static final EntityDataSerializer<Optional<BlockState>> OPTIONAL_BLOCK_STATE = EntityDataSerializer.forValueType(OPTIONAL_BLOCK_STATE_CODEC);
    public static final EntityDataSerializer<Boolean> BOOLEAN = EntityDataSerializer.forValueType(ByteBufCodecs.BOOL);
    public static final EntityDataSerializer<ParticleOptions> PARTICLE = EntityDataSerializer.forValueType(ParticleTypes.STREAM_CODEC);
    public static final EntityDataSerializer<List<ParticleOptions>> PARTICLES = EntityDataSerializer.forValueType(ParticleTypes.STREAM_CODEC.apply(ByteBufCodecs.list()));
    public static final EntityDataSerializer<Rotations> ROTATIONS = EntityDataSerializer.forValueType(Rotations.STREAM_CODEC);
    public static final EntityDataSerializer<BlockPos> BLOCK_POS = EntityDataSerializer.forValueType(BlockPos.STREAM_CODEC);
    public static final EntityDataSerializer<Optional<BlockPos>> OPTIONAL_BLOCK_POS = EntityDataSerializer.forValueType(BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional));
    public static final EntityDataSerializer<Direction> DIRECTION = EntityDataSerializer.forValueType(Direction.STREAM_CODEC);
    public static final EntityDataSerializer<Optional<EntityReference<LivingEntity>>> OPTIONAL_LIVING_ENTITY_REFERENCE = EntityDataSerializer.forValueType(EntityReference.streamCodec().apply(ByteBufCodecs::optional));
    public static final EntityDataSerializer<Optional<GlobalPos>> OPTIONAL_GLOBAL_POS = EntityDataSerializer.forValueType(GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional));
    public static final EntityDataSerializer<VillagerData> VILLAGER_DATA = EntityDataSerializer.forValueType(VillagerData.STREAM_CODEC);
    private static final StreamCodec<ByteBuf, OptionalInt> OPTIONAL_UNSIGNED_INT_CODEC = new StreamCodec<ByteBuf, OptionalInt>(){

        @Override
        public OptionalInt decode(ByteBuf input) {
            int v = VarInt.read(input);
            return v == 0 ? OptionalInt.empty() : OptionalInt.of(v - 1);
        }

        @Override
        public void encode(ByteBuf output, OptionalInt value) {
            VarInt.write(output, value.orElse(-1) + 1);
        }
    };
    public static final EntityDataSerializer<OptionalInt> OPTIONAL_UNSIGNED_INT = EntityDataSerializer.forValueType(OPTIONAL_UNSIGNED_INT_CODEC);
    public static final EntityDataSerializer<Pose> POSE = EntityDataSerializer.forValueType(Pose.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<CatVariant>> CAT_VARIANT = EntityDataSerializer.forValueType(CatVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<CatSoundVariant>> CAT_SOUND_VARIANT = EntityDataSerializer.forValueType(CatSoundVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<ChickenVariant>> CHICKEN_VARIANT = EntityDataSerializer.forValueType(ChickenVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<ChickenSoundVariant>> CHICKEN_SOUND_VARIANT = EntityDataSerializer.forValueType(ChickenSoundVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<CowVariant>> COW_VARIANT = EntityDataSerializer.forValueType(CowVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<CowSoundVariant>> COW_SOUND_VARIANT = EntityDataSerializer.forValueType(CowSoundVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<WolfVariant>> WOLF_VARIANT = EntityDataSerializer.forValueType(WolfVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<WolfSoundVariant>> WOLF_SOUND_VARIANT = EntityDataSerializer.forValueType(WolfSoundVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<FrogVariant>> FROG_VARIANT = EntityDataSerializer.forValueType(FrogVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<PigVariant>> PIG_VARIANT = EntityDataSerializer.forValueType(PigVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<PigSoundVariant>> PIG_SOUND_VARIANT = EntityDataSerializer.forValueType(PigSoundVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<ZombieNautilusVariant>> ZOMBIE_NAUTILUS_VARIANT = EntityDataSerializer.forValueType(ZombieNautilusVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Holder<PaintingVariant>> PAINTING_VARIANT = EntityDataSerializer.forValueType(PaintingVariant.STREAM_CODEC);
    public static final EntityDataSerializer<Armadillo.ArmadilloState> ARMADILLO_STATE = EntityDataSerializer.forValueType(Armadillo.ArmadilloState.STREAM_CODEC);
    public static final EntityDataSerializer<Sniffer.State> SNIFFER_STATE = EntityDataSerializer.forValueType(Sniffer.State.STREAM_CODEC);
    public static final EntityDataSerializer<WeatheringCopper.WeatherState> WEATHERING_COPPER_STATE = EntityDataSerializer.forValueType(WeatheringCopper.WeatherState.STREAM_CODEC);
    public static final EntityDataSerializer<CopperGolemState> COPPER_GOLEM_STATE = EntityDataSerializer.forValueType(CopperGolemState.STREAM_CODEC);
    public static final EntityDataSerializer<Vector3fc> VECTOR3 = EntityDataSerializer.forValueType(ByteBufCodecs.VECTOR3F);
    public static final EntityDataSerializer<Quaternionfc> QUATERNION = EntityDataSerializer.forValueType(ByteBufCodecs.QUATERNIONF);
    public static final EntityDataSerializer<ResolvableProfile> RESOLVABLE_PROFILE = EntityDataSerializer.forValueType(ResolvableProfile.STREAM_CODEC);
    public static final EntityDataSerializer<HumanoidArm> HUMANOID_ARM = EntityDataSerializer.forValueType(HumanoidArm.STREAM_CODEC);

    public static void registerSerializer(EntityDataSerializer<?> serializer) {
        SERIALIZERS.add(serializer);
    }

    public static @Nullable EntityDataSerializer<?> getSerializer(int id) {
        return SERIALIZERS.byId(id);
    }

    public static int getSerializedId(EntityDataSerializer<?> serializer) {
        return SERIALIZERS.getId(serializer);
    }

    private EntityDataSerializers() {
    }

    static {
        EntityDataSerializers.registerSerializer(BYTE);
        EntityDataSerializers.registerSerializer(INT);
        EntityDataSerializers.registerSerializer(LONG);
        EntityDataSerializers.registerSerializer(FLOAT);
        EntityDataSerializers.registerSerializer(STRING);
        EntityDataSerializers.registerSerializer(COMPONENT);
        EntityDataSerializers.registerSerializer(OPTIONAL_COMPONENT);
        EntityDataSerializers.registerSerializer(ITEM_STACK);
        EntityDataSerializers.registerSerializer(BOOLEAN);
        EntityDataSerializers.registerSerializer(ROTATIONS);
        EntityDataSerializers.registerSerializer(BLOCK_POS);
        EntityDataSerializers.registerSerializer(OPTIONAL_BLOCK_POS);
        EntityDataSerializers.registerSerializer(DIRECTION);
        EntityDataSerializers.registerSerializer(OPTIONAL_LIVING_ENTITY_REFERENCE);
        EntityDataSerializers.registerSerializer(BLOCK_STATE);
        EntityDataSerializers.registerSerializer(OPTIONAL_BLOCK_STATE);
        EntityDataSerializers.registerSerializer(PARTICLE);
        EntityDataSerializers.registerSerializer(PARTICLES);
        EntityDataSerializers.registerSerializer(VILLAGER_DATA);
        EntityDataSerializers.registerSerializer(OPTIONAL_UNSIGNED_INT);
        EntityDataSerializers.registerSerializer(POSE);
        EntityDataSerializers.registerSerializer(CAT_VARIANT);
        EntityDataSerializers.registerSerializer(CAT_SOUND_VARIANT);
        EntityDataSerializers.registerSerializer(COW_VARIANT);
        EntityDataSerializers.registerSerializer(COW_SOUND_VARIANT);
        EntityDataSerializers.registerSerializer(WOLF_VARIANT);
        EntityDataSerializers.registerSerializer(WOLF_SOUND_VARIANT);
        EntityDataSerializers.registerSerializer(FROG_VARIANT);
        EntityDataSerializers.registerSerializer(PIG_VARIANT);
        EntityDataSerializers.registerSerializer(PIG_SOUND_VARIANT);
        EntityDataSerializers.registerSerializer(CHICKEN_VARIANT);
        EntityDataSerializers.registerSerializer(CHICKEN_SOUND_VARIANT);
        EntityDataSerializers.registerSerializer(ZOMBIE_NAUTILUS_VARIANT);
        EntityDataSerializers.registerSerializer(OPTIONAL_GLOBAL_POS);
        EntityDataSerializers.registerSerializer(PAINTING_VARIANT);
        EntityDataSerializers.registerSerializer(SNIFFER_STATE);
        EntityDataSerializers.registerSerializer(ARMADILLO_STATE);
        EntityDataSerializers.registerSerializer(COPPER_GOLEM_STATE);
        EntityDataSerializers.registerSerializer(WEATHERING_COPPER_STATE);
        EntityDataSerializers.registerSerializer(VECTOR3);
        EntityDataSerializers.registerSerializer(QUATERNION);
        EntityDataSerializers.registerSerializer(RESOLVABLE_PROFILE);
        EntityDataSerializers.registerSerializer(HUMANOID_ARM);
    }
}

