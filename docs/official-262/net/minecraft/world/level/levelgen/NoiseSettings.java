/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.dimension.DimensionType;

public record NoiseSettings(int minY, int height, int noiseSizeHorizontal, int noiseSizeVertical) {
    public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.intRange((int)DimensionType.MIN_Y, (int)DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY), (App)Codec.intRange((int)0, (int)DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height), (App)Codec.intRange((int)1, (int)4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal), (App)Codec.intRange((int)1, (int)4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical)).apply((Applicative)i, NoiseSettings::new)).comapFlatMap(NoiseSettings::guardY, Function.identity());
    static final NoiseSettings OVERWORLD_NOISE_SETTINGS = NoiseSettings.create(-64, 384, 1, 2);
    static final NoiseSettings NETHER_NOISE_SETTINGS = NoiseSettings.create(0, 128, 1, 2);
    static final NoiseSettings END_NOISE_SETTINGS = NoiseSettings.create(0, 128, 2, 1);
    static final NoiseSettings CAVES_NOISE_SETTINGS = NoiseSettings.create(-64, 192, 1, 2);
    static final NoiseSettings FLOATING_ISLANDS_NOISE_SETTINGS = NoiseSettings.create(0, 256, 2, 1);

    private static DataResult<NoiseSettings> guardY(NoiseSettings dimensionType) {
        if (dimensionType.minY() + dimensionType.height() > DimensionType.MAX_Y + 1) {
            return DataResult.error(() -> "min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
        }
        if (dimensionType.height() % 16 != 0) {
            return DataResult.error(() -> "height has to be a multiple of 16");
        }
        if (dimensionType.minY() % 16 != 0) {
            return DataResult.error(() -> "min_y has to be a multiple of 16");
        }
        return DataResult.success((Object)dimensionType);
    }

    public static NoiseSettings create(int minY, int height, int noiseSizeHorizontal, int noiseSizeVertical) {
        NoiseSettings noiseSettings = new NoiseSettings(minY, height, noiseSizeHorizontal, noiseSizeVertical);
        NoiseSettings.guardY(noiseSettings).error().ifPresent(error -> {
            throw new IllegalStateException(error.message());
        });
        return noiseSettings;
    }

    public int getCellHeight() {
        return QuartPos.toBlock(this.noiseSizeVertical());
    }

    public int getCellWidth() {
        return QuartPos.toBlock(this.noiseSizeHorizontal());
    }

    public NoiseSettings clampToHeightAccessor(LevelHeightAccessor heightAccessor) {
        int newMinY = Math.max(this.minY, heightAccessor.getMinY());
        int newHeight = Math.min(this.minY + this.height, heightAccessor.getMaxY() + 1) - newMinY;
        return new NoiseSettings(newMinY, newHeight, this.noiseSizeHorizontal, this.noiseSizeVertical);
    }
}

