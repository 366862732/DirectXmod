/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.saveddata.maps;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import org.jspecify.annotations.Nullable;

public record MapBanner(BlockPos pos, DyeColor color, Optional<Component> name) {
    public static final Codec<MapBanner> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockPos.CODEC.fieldOf("pos").forGetter(MapBanner::pos), (App)DyeColor.CODEC.lenientOptionalFieldOf("color", DyeColor.WHITE).forGetter(MapBanner::color), (App)ComponentSerialization.CODEC.lenientOptionalFieldOf("name").forGetter(MapBanner::name)).apply((Applicative)i, MapBanner::new));

    public static @Nullable MapBanner fromWorld(BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof BannerBlockEntity) {
            BannerBlockEntity banner = (BannerBlockEntity)entity;
            DyeColor color = banner.getBaseColor();
            Optional<Component> name = Optional.ofNullable(banner.getCustomName());
            return new MapBanner(pos, color, name);
        }
        return null;
    }

    public Holder<MapDecorationType> getDecoration() {
        return switch (this.color) {
            default -> throw new MatchException(null, null);
            case DyeColor.WHITE -> MapDecorationTypes.WHITE_BANNER;
            case DyeColor.ORANGE -> MapDecorationTypes.ORANGE_BANNER;
            case DyeColor.MAGENTA -> MapDecorationTypes.MAGENTA_BANNER;
            case DyeColor.LIGHT_BLUE -> MapDecorationTypes.LIGHT_BLUE_BANNER;
            case DyeColor.YELLOW -> MapDecorationTypes.YELLOW_BANNER;
            case DyeColor.LIME -> MapDecorationTypes.LIME_BANNER;
            case DyeColor.PINK -> MapDecorationTypes.PINK_BANNER;
            case DyeColor.GRAY -> MapDecorationTypes.GRAY_BANNER;
            case DyeColor.LIGHT_GRAY -> MapDecorationTypes.LIGHT_GRAY_BANNER;
            case DyeColor.CYAN -> MapDecorationTypes.CYAN_BANNER;
            case DyeColor.PURPLE -> MapDecorationTypes.PURPLE_BANNER;
            case DyeColor.BLUE -> MapDecorationTypes.BLUE_BANNER;
            case DyeColor.BROWN -> MapDecorationTypes.BROWN_BANNER;
            case DyeColor.GREEN -> MapDecorationTypes.GREEN_BANNER;
            case DyeColor.RED -> MapDecorationTypes.RED_BANNER;
            case DyeColor.BLACK -> MapDecorationTypes.BLACK_BANNER;
        };
    }

    public String getId() {
        return "banner-" + this.pos.getX() + "," + this.pos.getY() + "," + this.pos.getZ();
    }
}

