/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.RewriteResult
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.View
 *  com.mojang.datafixers.functions.PointFreeRule
 *  com.mojang.datafixers.types.Type
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Util;

public class ExtraDataFixUtils {
    public static Dynamic<?> fixBlockPos(Dynamic<?> pos) {
        Optional x = pos.get("X").asNumber().result();
        Optional y = pos.get("Y").asNumber().result();
        Optional z = pos.get("Z").asNumber().result();
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return pos;
        }
        return ExtraDataFixUtils.createBlockPos(pos, ((Number)x.get()).intValue(), ((Number)y.get()).intValue(), ((Number)z.get()).intValue());
    }

    public static Dynamic<?> fixInlineBlockPos(Dynamic<?> input, String fieldX, String fieldY, String fieldZ, String newField) {
        Optional x = input.get(fieldX).asNumber().result();
        Optional y = input.get(fieldY).asNumber().result();
        Optional z = input.get(fieldZ).asNumber().result();
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return input;
        }
        return input.remove(fieldX).remove(fieldY).remove(fieldZ).set(newField, ExtraDataFixUtils.createBlockPos(input, ((Number)x.get()).intValue(), ((Number)y.get()).intValue(), ((Number)z.get()).intValue()));
    }

    public static Dynamic<?> createBlockPos(Dynamic<?> dynamic, int x, int y, int z) {
        return dynamic.createIntList(IntStream.of(x, y, z));
    }

    public static <T, R> Typed<R> cast(Type<R> type, Typed<T> typed) {
        return new Typed(type, typed.getOps(), typed.getValue());
    }

    public static <T> Typed<T> cast(Type<T> type, Object value, DynamicOps<?> ops) {
        return new Typed(type, ops, value);
    }

    public static Type<?> patchSubType(Type<?> type, Type<?> find, Type<?> replace) {
        return type.all(ExtraDataFixUtils.typePatcher(find, replace), true, false).view().newType();
    }

    private static <A, B> TypeRewriteRule typePatcher(Type<A> inputEntityType, Type<B> outputEntityType) {
        RewriteResult view = RewriteResult.create((View)View.create((String)"Patcher", inputEntityType, outputEntityType, ops -> a -> {
            throw new UnsupportedOperationException();
        }), (BitSet)new BitSet());
        return TypeRewriteRule.everywhere((TypeRewriteRule)TypeRewriteRule.ifSame(inputEntityType, (RewriteResult)view), (PointFreeRule)PointFreeRule.nop(), (boolean)true, (boolean)true);
    }

    @SafeVarargs
    public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>> ... fixers) {
        return typed -> {
            for (Function fixer : fixers) {
                typed = (Typed)fixer.apply(typed);
            }
            return typed;
        };
    }

    public static Dynamic<?> blockState(String id, Map<String, String> properties) {
        Dynamic dynamic = new Dynamic((DynamicOps)NbtOps.INSTANCE, (Object)new CompoundTag());
        Dynamic blockState = dynamic.set("Name", dynamic.createString(id));
        if (!properties.isEmpty()) {
            blockState = blockState.set("Properties", dynamic.createMap(properties.entrySet().stream().collect(Collectors.toMap(entry -> dynamic.createString((String)entry.getKey()), entry -> dynamic.createString((String)entry.getValue())))));
        }
        return blockState;
    }

    public static Dynamic<?> blockState(String id) {
        return ExtraDataFixUtils.blockState(id, Map.of());
    }

    public static Dynamic<?> fixStringField(Dynamic<?> dynamic, String fieldName, UnaryOperator<String> fix) {
        return dynamic.update(fieldName, field -> (Dynamic)DataFixUtils.orElse((Optional)field.asString().map((Function)fix).map(arg_0 -> ((Dynamic)dynamic).createString(arg_0)).result(), (Object)field));
    }

    public static String dyeColorIdToName(int id) {
        return switch (id) {
            default -> "white";
            case 1 -> "orange";
            case 2 -> "magenta";
            case 3 -> "light_blue";
            case 4 -> "yellow";
            case 5 -> "lime";
            case 6 -> "pink";
            case 7 -> "gray";
            case 8 -> "light_gray";
            case 9 -> "cyan";
            case 10 -> "purple";
            case 11 -> "blue";
            case 12 -> "brown";
            case 13 -> "green";
            case 14 -> "red";
            case 15 -> "black";
        };
    }

    public static <T> Typed<?> readAndSet(Typed<?> target, OpticFinder<T> optic, Dynamic<?> value) {
        return target.set(optic, Util.readTypedOrThrow(optic.type(), value, true));
    }
}

