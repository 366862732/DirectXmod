/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.NeedleDirectionHelper;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.MoonPhase;

@Environment(value=EnvType.CLIENT)
public class Time
extends NeedleDirectionHelper
implements RangeSelectItemModelProperty {
    public static final MapCodec<Time> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.optionalFieldOf("wobble", (Object)true).forGetter(NeedleDirectionHelper::wobble), (App)TimeSource.CODEC.fieldOf("source").forGetter(o -> o.source)).apply((Applicative)i, Time::new));
    private final TimeSource source;
    private final RandomSource randomSource = RandomSource.create();
    private final NeedleDirectionHelper.Wobbler wobbler;

    public Time(boolean wooble, TimeSource source) {
        super(wooble);
        this.source = source;
        this.wobbler = this.newWobbler(0.9f);
    }

    @Override
    protected float calculate(ItemStack itemStack, ClientLevel level, int seed, ItemOwner owner) {
        float targetRotation = this.source.get(level, itemStack, owner, this.randomSource);
        long gameTime = level.getGameTime();
        if (this.wobbler.shouldUpdate(gameTime)) {
            this.wobbler.update(gameTime, targetRotation);
        }
        return this.wobbler.rotation();
    }

    public MapCodec<Time> type() {
        return MAP_CODEC;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum TimeSource implements StringRepresentable
    {
        RANDOM("random"){

            @Override
            public float get(ClientLevel level, ItemStack itemStack, ItemOwner owner, RandomSource random) {
                return random.nextFloat();
            }
        }
        ,
        DAYTIME("daytime"){

            @Override
            public float get(ClientLevel level, ItemStack itemStack, ItemOwner owner, RandomSource random) {
                return level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, owner.position()).floatValue() / 360.0f;
            }
        }
        ,
        MOON_PHASE("moon_phase"){

            @Override
            public float get(ClientLevel level, ItemStack itemStack, ItemOwner owner, RandomSource random) {
                return (float)level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, owner.position()).index() / (float)MoonPhase.COUNT;
            }
        };

        public static final Codec<TimeSource> CODEC;
        private final String name;

        private TimeSource(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public abstract float get(ClientLevel var1, ItemStack var2, ItemOwner var3, RandomSource var4);

        static {
            CODEC = StringRepresentable.fromEnum(TimeSource::values);
        }
    }
}

