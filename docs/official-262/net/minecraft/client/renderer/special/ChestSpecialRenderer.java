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
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class ChestSpecialRenderer
implements NoDataSpecialModelRenderer {
    public static final Identifier ENDER_CHEST = Identifier.withDefaultNamespace("ender");
    public static final MultiblockChestResources<Identifier> REGULAR = ChestSpecialRenderer.createDefaultTextures("normal");
    public static final MultiblockChestResources<Identifier> TRAPPED = ChestSpecialRenderer.createDefaultTextures("trapped");
    public static final MultiblockChestResources<Identifier> CHRISTMAS = ChestSpecialRenderer.createDefaultTextures("christmas");
    public static final WeatheringCopperCollection.ByState<MultiblockChestResources<Identifier>> COPPER = new WeatheringCopperCollection.ByState<MultiblockChestResources<Identifier>>(ChestSpecialRenderer.createDefaultTextures("copper"), ChestSpecialRenderer.createDefaultTextures("copper_exposed"), ChestSpecialRenderer.createDefaultTextures("copper_weathered"), ChestSpecialRenderer.createDefaultTextures("copper_oxidized"));
    private final SpriteGetter sprites;
    private final ChestModel model;
    private final SpriteId sprite;
    private final float openness;

    public ChestSpecialRenderer(SpriteGetter sprites, ChestModel model, SpriteId sprite, float openness) {
        this.sprites = sprites;
        this.model = model;
        this.sprite = sprite;
        this.openness = openness;
    }

    private static MultiblockChestResources<Identifier> createDefaultTextures(String prefix) {
        return new MultiblockChestResources<Identifier>(Identifier.withDefaultNamespace(prefix), Identifier.withDefaultNamespace(prefix + "_left"), Identifier.withDefaultNamespace(prefix + "_right"));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        submitNodeCollector.submitModel(this.model, Float.valueOf(this.openness), poseStack, lightCoords, overlayCoords, -1, this.sprite, this.sprites, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(Float.valueOf(this.openness));
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @Environment(value=EnvType.CLIENT)
    public record Unbaked(Identifier texture, float openness, ChestType chestType) implements NoDataSpecialModelRenderer.Unbaked
    {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture), (App)Codec.FLOAT.optionalFieldOf("openness", (Object)Float.valueOf(0.0f)).forGetter(Unbaked::openness), (App)ChestType.CODEC.optionalFieldOf("chest_type", (Object)ChestType.SINGLE).forGetter(Unbaked::chestType)).apply((Applicative)i, Unbaked::new));

        public Unbaked(Identifier texture, ChestType chestType) {
            this(texture, 0.0f, chestType);
        }

        public Unbaked(Identifier texture) {
            this(texture, 0.0f, ChestType.SINGLE);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        public ChestSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            ChestModel model = new ChestModel(context.entityModelSet().bakeLayer(ChestRenderer.LAYERS.select(this.chestType)));
            SpriteId fullTexture = Sheets.CHEST_MAPPER.apply(this.texture);
            return new ChestSpecialRenderer(context.sprites(), model, fullTexture, this.openness);
        }
    }
}

