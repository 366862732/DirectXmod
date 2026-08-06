/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.lang3.StringUtils
 */
package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;

public class WorldOptions {
    public static final MapCodec<WorldOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.LONG.fieldOf("seed").stable().forGetter(WorldOptions::seed), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "generate_structures", true).stable().forGetter(WorldOptions::generateStructures), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "bonus_chest", false).stable().forGetter(WorldOptions::generateBonusChest), (App)Codec.STRING.lenientOptionalFieldOf("legacy_custom_options").stable().forGetter(s -> s.legacyCustomOptions)).apply((Applicative)i, i.stable(WorldOptions::new)));
    public static final WorldOptions DEMO_OPTIONS = new WorldOptions("North Carolina".hashCode(), true, true);
    private final long seed;
    private final boolean generateStructures;
    private final boolean generateBonusChest;
    private final Optional<String> legacyCustomOptions;

    public WorldOptions(long seed, boolean generateStructures, boolean generateBonusChest) {
        this(seed, generateStructures, generateBonusChest, Optional.empty());
    }

    public static WorldOptions defaultWithRandomSeed() {
        return new WorldOptions(WorldOptions.randomSeed(), true, false);
    }

    public static WorldOptions testWorldWithRandomSeed() {
        return new WorldOptions(WorldOptions.randomSeed(), false, false);
    }

    private WorldOptions(long seed, boolean generateStructures, boolean generateBonusChest, Optional<String> legacyCustomOptions) {
        this.seed = seed;
        this.generateStructures = generateStructures;
        this.generateBonusChest = generateBonusChest;
        this.legacyCustomOptions = legacyCustomOptions;
    }

    public long seed() {
        return this.seed;
    }

    public boolean generateStructures() {
        return this.generateStructures;
    }

    public boolean generateBonusChest() {
        return this.generateBonusChest;
    }

    public boolean isOldCustomizedWorld() {
        return this.legacyCustomOptions.isPresent();
    }

    public WorldOptions withBonusChest(boolean generateBonusChest) {
        return new WorldOptions(this.seed, this.generateStructures, generateBonusChest, this.legacyCustomOptions);
    }

    public WorldOptions withStructures(boolean generateStructures) {
        return new WorldOptions(this.seed, generateStructures, this.generateBonusChest, this.legacyCustomOptions);
    }

    public WorldOptions withSeed(OptionalLong seed) {
        return new WorldOptions(seed.orElse(WorldOptions.randomSeed()), this.generateStructures, this.generateBonusChest, this.legacyCustomOptions);
    }

    public static OptionalLong parseSeed(String seedString) {
        if (StringUtils.isEmpty((CharSequence)(seedString = seedString.trim()))) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(seedString));
        }
        catch (NumberFormatException e) {
            return OptionalLong.of(seedString.hashCode());
        }
    }

    public static long randomSeed() {
        return RandomSource.create().nextLong();
    }
}

