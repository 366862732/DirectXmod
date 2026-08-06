/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public abstract class CombiningPredicate
implements BlockPredicate {
    protected final List<BlockPredicate> predicates;

    protected CombiningPredicate(List<BlockPredicate> predicates) {
        this.predicates = predicates;
    }

    public static <T extends CombiningPredicate> MapCodec<T> codec(Function<List<BlockPredicate>, T> constructor) {
        return RecordCodecBuilder.mapCodec(i -> i.group((App)BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(p -> p.predicates)).apply((Applicative)i, constructor));
    }
}

