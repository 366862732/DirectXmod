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
package net.minecraft.world.level.dimension;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.timeline.Timeline;

public record DimensionType(boolean hasFixedTime, boolean hasSkyLight, boolean hasCeiling, boolean hasEnderDragonFight, double coordinateScale, int minY, int height, int logicalHeight, HolderSet<Block> infiniburn, float ambientLight, MonsterSettings monsterSettings, Skybox skybox, CardinalLighting.Type cardinalLightType, EnvironmentAttributeMap attributes, HolderSet<Timeline> timelines, Optional<Holder<WorldClock>> defaultClock) {
    public static final int BITS_FOR_Y = BlockPos.PACKED_Y_LENGTH;
    public static final int MIN_HEIGHT = 16;
    public static final int Y_SIZE = (1 << BITS_FOR_Y) - 32;
    public static final int MAX_Y = (Y_SIZE >> 1) - 1;
    public static final int MIN_Y = MAX_Y - Y_SIZE + 1;
    public static final int WAY_ABOVE_MAX_Y = MAX_Y << 4;
    public static final int WAY_BELOW_MIN_Y = MIN_Y << 4;
    public static final Codec<DimensionType> DIRECT_CODEC = DimensionType.createDirectCodec(EnvironmentAttributeMap.CODEC);
    public static final Codec<DimensionType> NETWORK_CODEC = DimensionType.createDirectCodec(EnvironmentAttributeMap.NETWORK_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DimensionType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DIMENSION_TYPE);
    public static final float[] MOON_BRIGHTNESS_PER_PHASE = new float[]{1.0f, 0.75f, 0.5f, 0.25f, 0.0f, 0.25f, 0.5f, 0.75f};
    public static final Codec<Holder<DimensionType>> CODEC = RegistryFileCodec.create(Registries.DIMENSION_TYPE, DIRECT_CODEC);

    public DimensionType {
        if (height < 16) {
            throw new IllegalStateException("height has to be at least 16");
        }
        if (minY + height > MAX_Y + 1) {
            throw new IllegalStateException("min_y + height cannot be higher than: " + (MAX_Y + 1));
        }
        if (logicalHeight > height) {
            throw new IllegalStateException("logical_height cannot be higher than height");
        }
        if (height % 16 != 0) {
            throw new IllegalStateException("height has to be multiple of 16");
        }
        if (minY % 16 != 0) {
            throw new IllegalStateException("min_y has to be a multiple of 16");
        }
    }

    private static Codec<DimensionType> createDirectCodec(Codec<EnvironmentAttributeMap> attributeMapCodec) {
        return ExtraCodecs.catchDecoderException(RecordCodecBuilder.create(i -> i.group((App)Codec.BOOL.optionalFieldOf("has_fixed_time", (Object)false).forGetter(DimensionType::hasFixedTime), (App)Codec.BOOL.fieldOf("has_skylight").forGetter(DimensionType::hasSkyLight), (App)Codec.BOOL.fieldOf("has_ceiling").forGetter(DimensionType::hasCeiling), (App)Codec.BOOL.fieldOf("has_ender_dragon_fight").forGetter(DimensionType::hasEnderDragonFight), (App)Codec.doubleRange((double)1.0E-5f, (double)3.0E7).fieldOf("coordinate_scale").forGetter(DimensionType::coordinateScale), (App)Codec.intRange((int)MIN_Y, (int)MAX_Y).fieldOf("min_y").forGetter(DimensionType::minY), (App)Codec.intRange((int)16, (int)Y_SIZE).fieldOf("height").forGetter(DimensionType::height), (App)Codec.intRange((int)0, (int)Y_SIZE).fieldOf("logical_height").forGetter(DimensionType::logicalHeight), (App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("infiniburn").forGetter(DimensionType::infiniburn), (App)Codec.FLOAT.fieldOf("ambient_light").forGetter(DimensionType::ambientLight), (App)MonsterSettings.CODEC.forGetter(DimensionType::monsterSettings), (App)Skybox.CODEC.optionalFieldOf("skybox", (Object)Skybox.OVERWORLD).forGetter(DimensionType::skybox), (App)CardinalLighting.Type.CODEC.optionalFieldOf("cardinal_light", (Object)CardinalLighting.Type.DEFAULT).forGetter(DimensionType::cardinalLightType), (App)attributeMapCodec.optionalFieldOf("attributes", (Object)EnvironmentAttributeMap.EMPTY).forGetter(DimensionType::attributes), (App)RegistryCodecs.homogeneousList(Registries.TIMELINE).optionalFieldOf("timelines", HolderSet.empty()).forGetter(DimensionType::timelines), (App)WorldClock.CODEC.optionalFieldOf("default_clock").forGetter(DimensionType::defaultClock)).apply((Applicative)i, DimensionType::new)));
    }

    public static double getTeleportationScale(DimensionType lastDimensionType, DimensionType newDimensionType) {
        double oldScale = lastDimensionType.coordinateScale();
        double newScale = newDimensionType.coordinateScale();
        return oldScale / newScale;
    }

    public static Path getStorageFolder(ResourceKey<Level> name, Path baseFolder) {
        return name.identifier().resolveAgainst(baseFolder.resolve("dimensions"));
    }

    public IntProvider monsterSpawnLightTest() {
        return this.monsterSettings.monsterSpawnLightTest();
    }

    public int monsterSpawnBlockLightLimit() {
        return this.monsterSettings.monsterSpawnBlockLightLimit();
    }

    public boolean hasEndFlashes() {
        return this.skybox == Skybox.END;
    }

    public record MonsterSettings(IntProvider monsterSpawnLightTest, int monsterSpawnBlockLightLimit) {
        public static final MapCodec<MonsterSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)IntProviders.codec(0, 15).fieldOf("monster_spawn_light_level").forGetter(MonsterSettings::monsterSpawnLightTest), (App)Codec.intRange((int)0, (int)15).fieldOf("monster_spawn_block_light_limit").forGetter(MonsterSettings::monsterSpawnBlockLightLimit)).apply((Applicative)i, MonsterSettings::new));
    }

    public static enum Skybox implements StringRepresentable
    {
        NONE("none"),
        OVERWORLD("overworld"),
        END("end");

        public static final Codec<Skybox> CODEC;
        private final String name;

        private Skybox(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Skybox::values);
        }
    }
}

