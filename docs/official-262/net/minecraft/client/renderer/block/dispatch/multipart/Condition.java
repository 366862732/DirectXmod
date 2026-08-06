/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Keyable
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.block.dispatch.multipart;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Keyable;
import java.lang.runtime.SwitchBootstraps;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.multipart.CombinedCondition;
import net.minecraft.client.renderer.block.dispatch.multipart.KeyValueCondition;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface Condition {
    public static final Codec<Condition> CODEC = Codec.recursive((String)"condition", self -> {
        Codec combinerCodec = Codec.simpleMap(CombinedCondition.Operation.CODEC, (Codec)self.listOf(), (Keyable)StringRepresentable.keys(CombinedCondition.Operation.values())).codec().comapFlatMap(map -> {
            if (map.size() != 1) {
                return DataResult.error(() -> "Invalid map size for combiner condition, expected exactly one element");
            }
            Map.Entry entry = map.entrySet().iterator().next();
            return DataResult.success((Object)new CombinedCondition((CombinedCondition.Operation)entry.getKey(), (List)entry.getValue()));
        }, condition -> Map.of(condition.operation(), condition.terms()));
        return Codec.either((Codec)combinerCodec, KeyValueCondition.CODEC).flatComapMap(either -> (Condition)either.map(l -> l, r -> r), condition -> {
            Condition condition2 = condition;
            Objects.requireNonNull(condition2);
            Condition selector0$temp = condition2;
            int index$1 = 0;
            DataResult result = switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{CombinedCondition.class, KeyValueCondition.class}, (Condition)selector0$temp, index$1)) {
                case 0 -> {
                    CombinedCondition combiner = (CombinedCondition)selector0$temp;
                    yield DataResult.success((Object)Either.left((Object)combiner));
                }
                case 1 -> {
                    KeyValueCondition keyValue = (KeyValueCondition)selector0$temp;
                    yield DataResult.success((Object)Either.right((Object)keyValue));
                }
                default -> DataResult.error(() -> "Unrecognized condition");
            };
            return result;
        });
    });

    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> var1);
}

