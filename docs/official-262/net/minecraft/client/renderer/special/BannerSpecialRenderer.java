/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BannerSpecialRenderer
implements SpecialModelRenderer<BannerPatternLayers> {
    private final BannerRenderer bannerRenderer;
    private final DyeColor baseColor;
    private final BannerBlock.AttachmentType attachment;

    public BannerSpecialRenderer(DyeColor baseColor, BannerRenderer bannerRenderer, BannerBlock.AttachmentType attachment) {
        this.bannerRenderer = bannerRenderer;
        this.baseColor = baseColor;
        this.attachment = attachment;
    }

    @Override
    public @Nullable BannerPatternLayers extractArgument(ItemStack stack) {
        return stack.get(DataComponents.BANNER_PATTERNS);
    }

    @Override
    public void submit(@Nullable BannerPatternLayers patterns, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        this.bannerRenderer.submitSpecial(this.attachment, poseStack, submitNodeCollector, lightCoords, overlayCoords, this.baseColor, Objects.requireNonNullElse(patterns, BannerPatternLayers.EMPTY), outlineColor);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        this.bannerRenderer.getExtents(output);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(DyeColor baseColor, BannerBlock.AttachmentType attachment) implements SpecialModelRenderer.Unbaked<BannerPatternLayers>
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)DyeColor.CODEC.fieldOf("color").forGetter(Unbaked::baseColor), (App)BannerBlock.AttachmentType.CODEC.optionalFieldOf("attachment", (Object)BannerBlock.AttachmentType.GROUND).forGetter(Unbaked::attachment)).apply((Applicative)i, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public BannerSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new BannerSpecialRenderer(this.baseColor, new BannerRenderer(context), this.attachment);
        }
    }
}

