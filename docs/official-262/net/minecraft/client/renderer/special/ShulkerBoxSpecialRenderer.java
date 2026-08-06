/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class ShulkerBoxSpecialRenderer
implements NoDataSpecialModelRenderer {
    private final ShulkerBoxRenderer shulkerBoxRenderer;
    private final float openness;
    private final SpriteId sprite;

    public ShulkerBoxSpecialRenderer(ShulkerBoxRenderer shulkerBoxRenderer, float openness, SpriteId sprite) {
        this.shulkerBoxRenderer = shulkerBoxRenderer;
        this.openness = openness;
        this.sprite = sprite;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        this.shulkerBoxRenderer.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, this.openness, null, this.sprite, outlineColor);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        this.shulkerBoxRenderer.getExtents(this.openness, output);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Identifier texture, float openness) implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture), (App)Codec.FLOAT.optionalFieldOf("openness", (Object)Float.valueOf(0.0f)).forGetter(Unbaked::openness)).apply((Applicative)i, Unbaked::new));

        public Unbaked() {
            this(Identifier.withDefaultNamespace("shulker"), 0.0f);
        }

        public Unbaked(DyeColor color) {
            this(Sheets.colorToShulkerSprite(color), 0.0f);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public ShulkerBoxSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ShulkerBoxSpecialRenderer(new ShulkerBoxRenderer(context), this.openness, Sheets.SHULKER_MAPPER.apply(this.texture));
        }
    }
}

