/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;

public class NopProcessor
implements StructureProcessor {
    public static final MapCodec<NopProcessor> MAP_CODEC = MapCodec.unit(() -> INSTANCE);
    public static final NopProcessor INSTANCE = new NopProcessor();

    private NopProcessor() {
    }

    public MapCodec<NopProcessor> codec() {
        return MAP_CODEC;
    }
}

