/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  org.apache.commons.lang3.function.TriFunction
 */
package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.function.TriFunction;

public record ColorCollection<T>(T white, T orange, T magenta, T lightBlue, T yellow, T lime, T pink, T gray, T lightGray, T cyan, T purple, T blue, T brown, T green, T red, T black) {
    public static final ColorCollection<DyeColor> VALUES = new ColorCollection<DyeColor>(DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE, DyeColor.BROWN, DyeColor.GREEN, DyeColor.RED, DyeColor.BLACK);
    public static final ColorCollection<String> NAMES = VALUES.map(DyeColor::getName);

    public static <T> ColorCollection<T> create(T value) {
        return new ColorCollection<T>(value, value, value, value, value, value, value, value, value, value, value, value, value, value, value, value);
    }

    public static <B extends Block, Id> ColorCollection<Block> registerBlocks(ColorCollection<Id> ids, TriFunction<Id, Function<BlockBehaviour.Properties, Block>, BlockBehaviour.Properties, Block> register, BiFunction<DyeColor, BlockBehaviour.Properties, B> colorBlockFactory, Function<DyeColor, BlockBehaviour.Properties> propertiesSupplier) {
        return ColorCollection.zipMap(VALUES, ids, (color, id) -> (Block)register.apply(id, p -> (Block)colorBlockFactory.apply((DyeColor)color, (BlockBehaviour.Properties)p), (Object)((BlockBehaviour.Properties)propertiesSupplier.apply((DyeColor)color))));
    }

    public static <Id> ColorCollection<Item> registerBlockItems(ColorCollection<Id> ids, ColorCollection<Block> blocks, TriFunction<Id, Block, DyeColor, Item> itemFactory) {
        return ColorCollection.zipMap(VALUES, ids, (color, id) -> (Item)itemFactory.apply(id, (Object)((Block)blocks.pick((DyeColor)color)), color));
    }

    public static <Id> ColorCollection<Item> registerItems(ColorCollection<Id> ids, BiFunction<Id, DyeColor, Item> itemFactory) {
        return ColorCollection.zipMap(VALUES, ids, (color, id) -> (Item)itemFactory.apply((Object)id, (DyeColor)color));
    }

    public static ColorCollection<String> prefixWithColor(ColorCollection<String> ids) {
        return ColorCollection.zipMap(NAMES, ids, (color, id) -> color + "_" + id);
    }

    public List<T> asList() {
        ImmutableList.Builder builder = ImmutableList.builderWithExpectedSize((int)16);
        this.forEach(arg_0 -> ((ImmutableList.Builder)builder).add(arg_0));
        return builder.build();
    }

    public void forEach(Consumer<T> consumer) {
        consumer.accept(this.white);
        consumer.accept(this.orange);
        consumer.accept(this.magenta);
        consumer.accept(this.lightBlue);
        consumer.accept(this.yellow);
        consumer.accept(this.lime);
        consumer.accept(this.pink);
        consumer.accept(this.gray);
        consumer.accept(this.lightGray);
        consumer.accept(this.cyan);
        consumer.accept(this.purple);
        consumer.accept(this.blue);
        consumer.accept(this.brown);
        consumer.accept(this.green);
        consumer.accept(this.red);
        consumer.accept(this.black);
    }

    public T pick(DyeColor dyeColor) {
        return switch (dyeColor) {
            default -> throw new MatchException(null, null);
            case DyeColor.WHITE -> this.white;
            case DyeColor.ORANGE -> this.orange;
            case DyeColor.MAGENTA -> this.magenta;
            case DyeColor.LIGHT_BLUE -> this.lightBlue;
            case DyeColor.YELLOW -> this.yellow;
            case DyeColor.LIME -> this.lime;
            case DyeColor.PINK -> this.pink;
            case DyeColor.GRAY -> this.gray;
            case DyeColor.LIGHT_GRAY -> this.lightGray;
            case DyeColor.CYAN -> this.cyan;
            case DyeColor.PURPLE -> this.purple;
            case DyeColor.BLUE -> this.blue;
            case DyeColor.BROWN -> this.brown;
            case DyeColor.GREEN -> this.green;
            case DyeColor.RED -> this.red;
            case DyeColor.BLACK -> this.black;
        };
    }

    public <U> ColorCollection<U> map(Function<T, U> mapper) {
        return new ColorCollection<U>(mapper.apply(this.white), mapper.apply(this.orange), mapper.apply(this.magenta), mapper.apply(this.lightBlue), mapper.apply(this.yellow), mapper.apply(this.lime), mapper.apply(this.pink), mapper.apply(this.gray), mapper.apply(this.lightGray), mapper.apply(this.cyan), mapper.apply(this.purple), mapper.apply(this.blue), mapper.apply(this.brown), mapper.apply(this.green), mapper.apply(this.red), mapper.apply(this.black));
    }

    public static <T, U> void zipApply(ColorCollection<T> first, ColorCollection<U> second, BiConsumer<T, U> consumer) {
        consumer.accept(first.white(), second.white());
        consumer.accept(first.orange(), second.orange());
        consumer.accept(first.magenta(), second.magenta());
        consumer.accept(first.lightBlue(), second.lightBlue());
        consumer.accept(first.yellow(), second.yellow());
        consumer.accept(first.lime(), second.lime());
        consumer.accept(first.pink(), second.pink());
        consumer.accept(first.gray(), second.gray());
        consumer.accept(first.lightGray(), second.lightGray());
        consumer.accept(first.cyan(), second.cyan());
        consumer.accept(first.purple(), second.purple());
        consumer.accept(first.blue(), second.blue());
        consumer.accept(first.brown(), second.brown());
        consumer.accept(first.green(), second.green());
        consumer.accept(first.red(), second.red());
        consumer.accept(first.black(), second.black());
    }

    public static <T, U, R> ColorCollection<R> zipMap(ColorCollection<T> first, ColorCollection<U> second, BiFunction<T, U, R> operation) {
        return new ColorCollection<R>(operation.apply(first.white(), second.white()), operation.apply(first.orange(), second.orange()), operation.apply(first.magenta(), second.magenta()), operation.apply(first.lightBlue(), second.lightBlue()), operation.apply(first.yellow(), second.yellow()), operation.apply(first.lime(), second.lime()), operation.apply(first.pink(), second.pink()), operation.apply(first.gray(), second.gray()), operation.apply(first.lightGray(), second.lightGray()), operation.apply(first.cyan(), second.cyan()), operation.apply(first.purple(), second.purple()), operation.apply(first.blue(), second.blue()), operation.apply(first.brown(), second.brown()), operation.apply(first.green(), second.green()), operation.apply(first.red(), second.red()), operation.apply(first.black(), second.black()));
    }
}

