/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch.multipart;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;

@Environment(value=EnvType.CLIENT)
public record Selector(Optional<Condition> condition, BlockStateModel.Unbaked variant) {
    public static final Codec<Selector> CODEC = RecordCodecBuilder.create(i -> i.group((App)Condition.CODEC.optionalFieldOf("when").forGetter(Selector::condition), (App)BlockStateModel.Unbaked.CODEC.fieldOf("apply").forGetter(Selector::variant)).apply((Applicative)i, Selector::new));

    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> definition) {
        return this.condition.map(c -> c.instantiate(definition)).orElse(state -> true);
    }
}

