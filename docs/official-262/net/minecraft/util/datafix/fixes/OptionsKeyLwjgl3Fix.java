/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.DataFixUtils
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.stream.Collectors;
import net.minecraft.util.datafix.fixes.References;

public class OptionsKeyLwjgl3Fix
extends DataFix {
    public static final String KEY_UNKNOWN = "key.unknown";
    private static final Int2ObjectMap<String> MAP = (Int2ObjectMap)DataFixUtils.make((Object)new Int2ObjectOpenHashMap(), map -> {
        map.put(0, (Object)KEY_UNKNOWN);
        map.put(11, (Object)"key.0");
        map.put(2, (Object)"key.1");
        map.put(3, (Object)"key.2");
        map.put(4, (Object)"key.3");
        map.put(5, (Object)"key.4");
        map.put(6, (Object)"key.5");
        map.put(7, (Object)"key.6");
        map.put(8, (Object)"key.7");
        map.put(9, (Object)"key.8");
        map.put(10, (Object)"key.9");
        map.put(30, (Object)"key.a");
        map.put(40, (Object)"key.apostrophe");
        map.put(48, (Object)"key.b");
        map.put(43, (Object)"key.backslash");
        map.put(14, (Object)"key.backspace");
        map.put(46, (Object)"key.c");
        map.put(58, (Object)"key.caps.lock");
        map.put(51, (Object)"key.comma");
        map.put(32, (Object)"key.d");
        map.put(211, (Object)"key.delete");
        map.put(208, (Object)"key.down");
        map.put(18, (Object)"key.e");
        map.put(207, (Object)"key.end");
        map.put(28, (Object)"key.enter");
        map.put(13, (Object)"key.equal");
        map.put(1, (Object)"key.escape");
        map.put(33, (Object)"key.f");
        map.put(59, (Object)"key.f1");
        map.put(68, (Object)"key.f10");
        map.put(87, (Object)"key.f11");
        map.put(88, (Object)"key.f12");
        map.put(100, (Object)"key.f13");
        map.put(101, (Object)"key.f14");
        map.put(102, (Object)"key.f15");
        map.put(103, (Object)"key.f16");
        map.put(104, (Object)"key.f17");
        map.put(105, (Object)"key.f18");
        map.put(113, (Object)"key.f19");
        map.put(60, (Object)"key.f2");
        map.put(61, (Object)"key.f3");
        map.put(62, (Object)"key.f4");
        map.put(63, (Object)"key.f5");
        map.put(64, (Object)"key.f6");
        map.put(65, (Object)"key.f7");
        map.put(66, (Object)"key.f8");
        map.put(67, (Object)"key.f9");
        map.put(34, (Object)"key.g");
        map.put(41, (Object)"key.grave.accent");
        map.put(35, (Object)"key.h");
        map.put(199, (Object)"key.home");
        map.put(23, (Object)"key.i");
        map.put(210, (Object)"key.insert");
        map.put(36, (Object)"key.j");
        map.put(37, (Object)"key.k");
        map.put(82, (Object)"key.keypad.0");
        map.put(79, (Object)"key.keypad.1");
        map.put(80, (Object)"key.keypad.2");
        map.put(81, (Object)"key.keypad.3");
        map.put(75, (Object)"key.keypad.4");
        map.put(76, (Object)"key.keypad.5");
        map.put(77, (Object)"key.keypad.6");
        map.put(71, (Object)"key.keypad.7");
        map.put(72, (Object)"key.keypad.8");
        map.put(73, (Object)"key.keypad.9");
        map.put(78, (Object)"key.keypad.add");
        map.put(83, (Object)"key.keypad.decimal");
        map.put(181, (Object)"key.keypad.divide");
        map.put(156, (Object)"key.keypad.enter");
        map.put(141, (Object)"key.keypad.equal");
        map.put(55, (Object)"key.keypad.multiply");
        map.put(74, (Object)"key.keypad.subtract");
        map.put(38, (Object)"key.l");
        map.put(203, (Object)"key.left");
        map.put(56, (Object)"key.left.alt");
        map.put(26, (Object)"key.left.bracket");
        map.put(29, (Object)"key.left.control");
        map.put(42, (Object)"key.left.shift");
        map.put(219, (Object)"key.left.win");
        map.put(50, (Object)"key.m");
        map.put(12, (Object)"key.minus");
        map.put(49, (Object)"key.n");
        map.put(69, (Object)"key.num.lock");
        map.put(24, (Object)"key.o");
        map.put(25, (Object)"key.p");
        map.put(209, (Object)"key.page.down");
        map.put(201, (Object)"key.page.up");
        map.put(197, (Object)"key.pause");
        map.put(52, (Object)"key.period");
        map.put(183, (Object)"key.print.screen");
        map.put(16, (Object)"key.q");
        map.put(19, (Object)"key.r");
        map.put(205, (Object)"key.right");
        map.put(184, (Object)"key.right.alt");
        map.put(27, (Object)"key.right.bracket");
        map.put(157, (Object)"key.right.control");
        map.put(54, (Object)"key.right.shift");
        map.put(220, (Object)"key.right.win");
        map.put(31, (Object)"key.s");
        map.put(70, (Object)"key.scroll.lock");
        map.put(39, (Object)"key.semicolon");
        map.put(53, (Object)"key.slash");
        map.put(57, (Object)"key.space");
        map.put(20, (Object)"key.t");
        map.put(15, (Object)"key.tab");
        map.put(22, (Object)"key.u");
        map.put(200, (Object)"key.up");
        map.put(47, (Object)"key.v");
        map.put(17, (Object)"key.w");
        map.put(45, (Object)"key.x");
        map.put(21, (Object)"key.y");
        map.put(44, (Object)"key.z");
    });

    public OptionsKeyLwjgl3Fix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("OptionsKeyLwjgl3Fix", this.getInputSchema().getType(References.OPTIONS), input -> input.update(DSL.remainderFinder(), tag -> tag.getMapValues().map(map1 -> tag.createMap(map1.entrySet().stream().map(entry -> {
            if (((Dynamic)entry.getKey()).asString("").startsWith("key_")) {
                int oldValue = Integer.parseInt(((Dynamic)entry.getValue()).asString(""));
                if (oldValue < 0) {
                    int button = oldValue + 100;
                    Object name = button == 0 ? "key.mouse.left" : (button == 1 ? "key.mouse.right" : (button == 2 ? "key.mouse.middle" : "key.mouse." + (button + 1)));
                    return Pair.of((Object)((Dynamic)entry.getKey()), (Object)((Dynamic)entry.getValue()).createString((String)name));
                }
                String name = (String)MAP.getOrDefault(oldValue, (Object)KEY_UNKNOWN);
                return Pair.of((Object)((Dynamic)entry.getKey()), (Object)((Dynamic)entry.getValue()).createString(name));
            }
            return Pair.of((Object)((Dynamic)entry.getKey()), (Object)((Dynamic)entry.getValue()));
        }).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)))).result().orElse(tag)));
    }
}

