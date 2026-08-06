/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 */
package net.minecraft.stats;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.RecipeBookType;

public final class RecipeBookSettings {
    public static final StreamCodec<FriendlyByteBuf, RecipeBookSettings> STREAM_CODEC = StreamCodec.composite(TypeSettings.STREAM_CODEC, o -> o.crafting, TypeSettings.STREAM_CODEC, o -> o.furnace, TypeSettings.STREAM_CODEC, o -> o.blastFurnace, TypeSettings.STREAM_CODEC, o -> o.smoker, RecipeBookSettings::new);
    public static final MapCodec<RecipeBookSettings> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)TypeSettings.CRAFTING_MAP_CODEC.forGetter(o -> o.crafting), (App)TypeSettings.FURNACE_MAP_CODEC.forGetter(o -> o.furnace), (App)TypeSettings.BLAST_FURNACE_MAP_CODEC.forGetter(o -> o.blastFurnace), (App)TypeSettings.SMOKER_MAP_CODEC.forGetter(o -> o.smoker)).apply((Applicative)i, RecipeBookSettings::new));
    private TypeSettings crafting;
    private TypeSettings furnace;
    private TypeSettings blastFurnace;
    private TypeSettings smoker;

    public RecipeBookSettings() {
        this(TypeSettings.DEFAULT, TypeSettings.DEFAULT, TypeSettings.DEFAULT, TypeSettings.DEFAULT);
    }

    private RecipeBookSettings(TypeSettings crafting, TypeSettings furnace, TypeSettings blastFurnace, TypeSettings smoker) {
        this.crafting = crafting;
        this.furnace = furnace;
        this.blastFurnace = blastFurnace;
        this.smoker = smoker;
    }

    @VisibleForTesting
    public TypeSettings getSettings(RecipeBookType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case RecipeBookType.CRAFTING -> this.crafting;
            case RecipeBookType.FURNACE -> this.furnace;
            case RecipeBookType.BLAST_FURNACE -> this.blastFurnace;
            case RecipeBookType.SMOKER -> this.smoker;
        };
    }

    private void updateSettings(RecipeBookType recipeBookType, UnaryOperator<TypeSettings> operator) {
        switch (recipeBookType) {
            case CRAFTING: {
                this.crafting = (TypeSettings)operator.apply(this.crafting);
                break;
            }
            case FURNACE: {
                this.furnace = (TypeSettings)operator.apply(this.furnace);
                break;
            }
            case BLAST_FURNACE: {
                this.blastFurnace = (TypeSettings)operator.apply(this.blastFurnace);
                break;
            }
            case SMOKER: {
                this.smoker = (TypeSettings)operator.apply(this.smoker);
            }
        }
    }

    public boolean isOpen(RecipeBookType type) {
        return this.getSettings((RecipeBookType)type).open;
    }

    public void setOpen(RecipeBookType type, boolean open) {
        this.updateSettings(type, s -> s.setOpen(open));
    }

    public boolean isFiltering(RecipeBookType type) {
        return this.getSettings((RecipeBookType)type).filtering;
    }

    public void setFiltering(RecipeBookType type, boolean filtering) {
        this.updateSettings(type, s -> s.setFiltering(filtering));
    }

    public RecipeBookSettings copy() {
        return new RecipeBookSettings(this.crafting, this.furnace, this.blastFurnace, this.smoker);
    }

    public void replaceFrom(RecipeBookSettings other) {
        this.crafting = other.crafting;
        this.furnace = other.furnace;
        this.blastFurnace = other.blastFurnace;
        this.smoker = other.smoker;
    }

    public record TypeSettings(boolean open, boolean filtering) {
        public static final TypeSettings DEFAULT = new TypeSettings(false, false);
        public static final MapCodec<TypeSettings> CRAFTING_MAP_CODEC = TypeSettings.codec("isGuiOpen", "isFilteringCraftable");
        public static final MapCodec<TypeSettings> FURNACE_MAP_CODEC = TypeSettings.codec("isFurnaceGuiOpen", "isFurnaceFilteringCraftable");
        public static final MapCodec<TypeSettings> BLAST_FURNACE_MAP_CODEC = TypeSettings.codec("isBlastingFurnaceGuiOpen", "isBlastingFurnaceFilteringCraftable");
        public static final MapCodec<TypeSettings> SMOKER_MAP_CODEC = TypeSettings.codec("isSmokerGuiOpen", "isSmokerFilteringCraftable");
        public static final StreamCodec<ByteBuf, TypeSettings> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, TypeSettings::open, ByteBufCodecs.BOOL, TypeSettings::filtering, TypeSettings::new);

        @Override
        public String toString() {
            return "[open=" + this.open + ", filtering=" + this.filtering + "]";
        }

        public TypeSettings setOpen(boolean open) {
            return new TypeSettings(open, this.filtering);
        }

        public TypeSettings setFiltering(boolean filtering) {
            return new TypeSettings(this.open, filtering);
        }

        private static MapCodec<TypeSettings> codec(String openFieldName, String filteringFieldName) {
            return RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.BOOL.optionalFieldOf(openFieldName, (Object)false).forGetter(TypeSettings::open), (App)Codec.BOOL.optionalFieldOf(filteringFieldName, (Object)false).forGetter(TypeSettings::filtering)).apply((Applicative)i, TypeSettings::new));
        }
    }
}

