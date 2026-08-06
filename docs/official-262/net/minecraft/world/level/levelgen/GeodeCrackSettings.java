/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;

public class GeodeCrackSettings {
    public static final Codec<GeodeCrackSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)GeodeConfiguration.CHANCE_RANGE.optionalFieldOf("generate_crack_chance", (Object)1.0).forGetter(c -> c.generateCrackChance), (App)Codec.doubleRange((double)0.0, (double)5.0).optionalFieldOf("base_crack_size", (Object)2.0).forGetter(c -> c.baseCrackSize), (App)Codec.intRange((int)0, (int)10).optionalFieldOf("crack_point_offset", (Object)2).forGetter(c -> c.crackPointOffset)).apply((Applicative)i, GeodeCrackSettings::new));
    public final double generateCrackChance;
    public final double baseCrackSize;
    public final int crackPointOffset;

    public GeodeCrackSettings(double generateCrackChance, double baseCrackSize, int crackPointOffset) {
        this.generateCrackChance = generateCrackChance;
        this.baseCrackSize = baseCrackSize;
        this.crackPointOffset = crackPointOffset;
    }
}

