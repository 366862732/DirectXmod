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

public class GeodeLayerSettings {
    private static final Codec<Double> LAYER_RANGE = Codec.doubleRange((double)0.01, (double)50.0);
    public static final Codec<GeodeLayerSettings> CODEC = RecordCodecBuilder.create(i -> i.group((App)LAYER_RANGE.optionalFieldOf("filling", (Object)1.7).forGetter(c -> c.filling), (App)LAYER_RANGE.optionalFieldOf("inner_layer", (Object)2.2).forGetter(c -> c.innerLayer), (App)LAYER_RANGE.optionalFieldOf("middle_layer", (Object)3.2).forGetter(c -> c.middleLayer), (App)LAYER_RANGE.optionalFieldOf("outer_layer", (Object)4.2).forGetter(c -> c.outerLayer)).apply((Applicative)i, GeodeLayerSettings::new));
    public final double filling;
    public final double innerLayer;
    public final double middleLayer;
    public final double outerLayer;

    public GeodeLayerSettings(double filling, double innerLayer, double middleLayer, double outerLayer) {
        this.filling = filling;
        this.innerLayer = innerLayer;
        this.middleLayer = middleLayer;
        this.outerLayer = outerLayer;
    }
}

