/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  io.netty.buffer.ByteBuf
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum DyeColor implements StringRepresentable
{
    WHITE(0, "white", 0xF9FFFE, MapColor.SNOW, MapColor.TERRACOTTA_WHITE, 0xF0F0F0, 0xFFFFFF),
    ORANGE(1, "orange", 16351261, MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_ORANGE, 15435844, 16738335),
    MAGENTA(2, "magenta", 13061821, MapColor.COLOR_MAGENTA, MapColor.TERRACOTTA_MAGENTA, 12801229, 0xFF00FF),
    LIGHT_BLUE(3, "light_blue", 3847130, MapColor.COLOR_LIGHT_BLUE, MapColor.TERRACOTTA_LIGHT_BLUE, 6719955, 10141901),
    YELLOW(4, "yellow", 16701501, MapColor.COLOR_YELLOW, MapColor.TERRACOTTA_YELLOW, 14602026, 0xFFFF00),
    LIME(5, "lime", 8439583, MapColor.COLOR_LIGHT_GREEN, MapColor.TERRACOTTA_LIGHT_GREEN, 4312372, 0xBFFF00),
    PINK(6, "pink", 15961002, MapColor.COLOR_PINK, MapColor.TERRACOTTA_PINK, 14188952, 16738740),
    GRAY(7, "gray", 4673362, MapColor.COLOR_GRAY, MapColor.TERRACOTTA_GRAY, 0x434343, 0x808080),
    LIGHT_GRAY(8, "light_gray", 0x9D9D97, MapColor.COLOR_LIGHT_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY, 0xABABAB, 0xD3D3D3),
    CYAN(9, "cyan", 1481884, MapColor.COLOR_CYAN, MapColor.TERRACOTTA_CYAN, 2651799, 65535),
    PURPLE(10, "purple", 8991416, MapColor.COLOR_PURPLE, MapColor.TERRACOTTA_PURPLE, 8073150, 10494192),
    BLUE(11, "blue", 3949738, MapColor.COLOR_BLUE, MapColor.TERRACOTTA_BLUE, 2437522, 255),
    BROWN(12, "brown", 8606770, MapColor.COLOR_BROWN, MapColor.TERRACOTTA_BROWN, 5320730, 9127187),
    GREEN(13, "green", 6192150, MapColor.COLOR_GREEN, MapColor.TERRACOTTA_GREEN, 3887386, 65280),
    RED(14, "red", 11546150, MapColor.COLOR_RED, MapColor.TERRACOTTA_RED, 11743532, 0xFF0000),
    BLACK(15, "black", 0x1D1D21, MapColor.COLOR_BLACK, MapColor.TERRACOTTA_BLACK, 0x1E1B1B, 0);

    public static final List<DyeColor> VALUES;
    private static final IntFunction<DyeColor> BY_ID;
    private static final Int2ObjectOpenHashMap<DyeColor> BY_FIREWORK_COLOR;
    public static final StringRepresentable.EnumCodec<DyeColor> CODEC;
    public static final StreamCodec<ByteBuf, DyeColor> STREAM_CODEC;
    @Deprecated
    public static final Codec<DyeColor> LEGACY_ID_CODEC;
    private final int id;
    private final String name;
    private final MapColor mapColor;
    private final MapColor terracottaColor;
    private final int textureDiffuseColor;
    private final int fireworkColor;
    private final int textColor;

    private DyeColor(int id, String name, int textureDiffuseColor, MapColor mapColor, MapColor terracottaColor, int fireworkColor, int textColor) {
        this.id = id;
        this.name = name;
        this.mapColor = mapColor;
        this.terracottaColor = terracottaColor;
        this.textColor = ARGB.opaque(textColor);
        this.textureDiffuseColor = ARGB.opaque(textureDiffuseColor);
        this.fireworkColor = fireworkColor;
    }

    public int getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getTextureDiffuseColor() {
        return this.textureDiffuseColor;
    }

    public MapColor getMapColor() {
        return this.mapColor;
    }

    public MapColor getTerracottaColor() {
        return this.terracottaColor;
    }

    public int getFireworkColor() {
        return this.fireworkColor;
    }

    public int getTextColor() {
        return this.textColor;
    }

    public static DyeColor byId(int id) {
        return BY_ID.apply(id);
    }

    @Contract(value="_,!null->!null;_,null->_")
    public static @Nullable DyeColor byName(String name, @Nullable DyeColor def) {
        DyeColor result = CODEC.byName(name);
        return result != null ? result : def;
    }

    public static @Nullable DyeColor byFireworkColor(int color) {
        return (DyeColor)BY_FIREWORK_COLOR.get(color);
    }

    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static DyeColor getMixedColor(ServerLevel level, DyeColor dyeColor1, DyeColor dyeColor2) {
        DyeColor mixedColor = DyeColor.findColorMixInRecipes(level, dyeColor1, dyeColor2);
        if (mixedColor != null) {
            return mixedColor;
        }
        return level.getRandom().nextBoolean() ? dyeColor1 : dyeColor2;
    }

    private static @Nullable DyeColor findColorMixInRecipes(ServerLevel level, DyeColor dyeColor1, DyeColor dyeColor2) {
        DataComponentLookup itemComponents = level.registryAccess().lookupOrThrow(Registries.ITEM).componentLookup();
        Collection dye1Items = itemComponents.findAll(DataComponents.DYE, dyeColor1);
        if (dye1Items.isEmpty()) {
            return null;
        }
        Collection dye2Items = itemComponents.findAll(DataComponents.DYE, dyeColor2);
        if (dye2Items.isEmpty()) {
            return null;
        }
        for (Holder<Item> holder : dye1Items) {
            for (Holder<Item> holder2 : dye2Items) {
                ItemStack craftingResult;
                DyeColor craftedDyeColor;
                CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(holder), new ItemStack(holder2)));
                Optional<RecipeHolder<CraftingRecipe>> foundRecipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level);
                if (!foundRecipe.isPresent() || (craftedDyeColor = (craftingResult = foundRecipe.get().value().assemble(input)).get(DataComponents.DYE)) == null) continue;
                return craftedDyeColor;
            }
        }
        return null;
    }

    static {
        VALUES = List.of(DyeColor.values());
        BY_ID = ByIdMap.continuous(DyeColor::getId, (DyeColor[])VALUES.toArray(DyeColor[]::new), ByIdMap.OutOfBoundsStrategy.ZERO);
        BY_FIREWORK_COLOR = new Int2ObjectOpenHashMap(VALUES.stream().collect(Collectors.toMap(v -> v.fireworkColor, v -> v)));
        CODEC = StringRepresentable.fromEnum(DyeColor::values);
        STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DyeColor::getId);
        LEGACY_ID_CODEC = Codec.BYTE.xmap(DyeColor::byId, color -> (byte)color.id);
    }
}

