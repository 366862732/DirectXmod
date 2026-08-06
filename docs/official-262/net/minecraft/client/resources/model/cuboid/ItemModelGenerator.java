/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.cuboid;

import com.mojang.math.Quadrant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ItemModelGenerator
implements UnbakedModel {
    public static final Identifier GENERATED_ITEM_MODEL_ID = Identifier.withDefaultNamespace("builtin/generated");
    public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
    private static final float MIN_Z = 7.5f;
    private static final float MAX_Z = 8.5f;
    private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();
    private static final CuboidFace.UVs SOUTH_FACE_UVS = new CuboidFace.UVs(0.0f, 0.0f, 16.0f, 16.0f);
    private static final CuboidFace.UVs NORTH_FACE_UVS = new CuboidFace.UVs(16.0f, 0.0f, 0.0f, 16.0f);
    private static final float UV_SHRINK = 0.1f;

    @Override
    public TextureSlots.Data textureSlots() {
        return TEXTURE_SLOTS;
    }

    @Override
    public UnbakedGeometry geometry() {
        return ItemModelGenerator::bake;
    }

    @Override
    public @Nullable UnbakedModel.GuiLight guiLight() {
        return UnbakedModel.GuiLight.FRONT;
    }

    private static QuadCollection bake(TextureSlots textureSlots, ModelBaker modelBaker, ModelState modelState, ModelDebugName name) {
        String textureReference;
        Material material;
        QuadCollection singleResult = null;
        QuadCollection.Builder builder = null;
        for (int layerIndex = 0; layerIndex < LAYERS.size() && (material = textureSlots.getMaterial(textureReference = LAYERS.get(layerIndex))) != null; ++layerIndex) {
            Material.Baked bakedMaterial = modelBaker.materials().get(material, name);
            QuadCollection bakedLayer = modelBaker.compute(new ItemLayerKey(bakedMaterial, modelState, layerIndex));
            if (builder != null) {
                builder.addAll(bakedLayer);
                continue;
            }
            if (singleResult != null) {
                builder = new QuadCollection.Builder();
                builder.addAll(singleResult);
                builder.addAll(bakedLayer);
                singleResult = null;
                continue;
            }
            singleResult = bakedLayer;
        }
        if (builder != null) {
            return builder.build();
        }
        return singleResult != null ? singleResult : QuadCollection.EMPTY;
    }

    private static void bakeExtrudedSprite(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        Vector3f from = new Vector3f(0.0f, 0.0f, 7.5f);
        Vector3f to = new Vector3f(16.0f, 16.0f, 8.5f);
        builder.addUnculledFace(FaceBakery.bakeQuad(interner, (Vector3fc)from, (Vector3fc)to, SOUTH_FACE_UVS, Quadrant.R0, materialInfo, Direction.SOUTH, modelState, null));
        builder.addUnculledFace(FaceBakery.bakeQuad(interner, (Vector3fc)from, (Vector3fc)to, NORTH_FACE_UVS, Quadrant.R0, materialInfo, Direction.NORTH, modelState, null));
        ItemModelGenerator.bakeSideFaces(builder, interner, modelState, materialInfo);
    }

    private static void bakeSideFaces(QuadCollection.Builder builder, ModelBaker.Interner interner, ModelState modelState, BakedQuad.MaterialInfo materialInfo) {
        SpriteContents sprite = materialInfo.sprite().contents();
        float xScale = 16.0f / (float)sprite.width();
        float yScale = 16.0f / (float)sprite.height();
        Vector3f from = new Vector3f();
        Vector3f to = new Vector3f();
        for (SideFace sideFace : ItemModelGenerator.getSideFaces(sprite)) {
            float v1;
            float v0;
            float x = sideFace.x();
            float y = sideFace.y();
            SideDirection sideDirection = sideFace.facing();
            float u0 = x + 0.1f;
            float u1 = x + 1.0f - 0.1f;
            if (sideDirection.isHorizontal()) {
                v0 = y + 0.1f;
                v1 = y + 1.0f - 0.1f;
            } else {
                v0 = y + 1.0f - 0.1f;
                v1 = y + 0.1f;
            }
            float startX = x;
            float startY = y;
            float endX = x;
            float endY = y;
            switch (sideDirection.ordinal()) {
                case 0: {
                    endX += 1.0f;
                    break;
                }
                case 1: {
                    endX += 1.0f;
                    startY += 1.0f;
                    endY += 1.0f;
                    break;
                }
                case 2: {
                    endY += 1.0f;
                    break;
                }
                case 3: {
                    startX += 1.0f;
                    endX += 1.0f;
                    endY += 1.0f;
                }
            }
            startX *= xScale;
            endX *= xScale;
            startY *= yScale;
            endY *= yScale;
            startY = 16.0f - startY;
            endY = 16.0f - endY;
            switch (sideDirection.ordinal()) {
                case 0: {
                    from.set(startX, startY, 7.5f);
                    to.set(endX, startY, 8.5f);
                    break;
                }
                case 1: {
                    from.set(startX, endY, 7.5f);
                    to.set(endX, endY, 8.5f);
                    break;
                }
                case 2: {
                    from.set(startX, startY, 7.5f);
                    to.set(startX, endY, 8.5f);
                    break;
                }
                case 3: {
                    from.set(endX, startY, 7.5f);
                    to.set(endX, endY, 8.5f);
                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
            CuboidFace.UVs uvs = new CuboidFace.UVs(u0 * xScale, v0 * yScale, u1 * xScale, v1 * yScale);
            builder.addUnculledFace(FaceBakery.bakeQuad(interner, (Vector3fc)from, (Vector3fc)to, uvs, Quadrant.R0, materialInfo, sideDirection.getDirection(), modelState, null));
        }
    }

    private static Collection<SideFace> getSideFaces(SpriteContents sprite) {
        int width = sprite.width();
        int height = sprite.height();
        HashSet<SideFace> sideFaces = new HashSet<SideFace>();
        sprite.getUniqueFrames().forEach(frame -> {
            for (int y = 0; y < height; ++y) {
                for (int x = 0; x < width; ++x) {
                    boolean thisOpaque;
                    boolean bl = thisOpaque = !ItemModelGenerator.isTransparent(sprite, frame, x, y, width, height);
                    if (!thisOpaque) continue;
                    ItemModelGenerator.checkTransition(SideDirection.UP, sideFaces, sprite, frame, x, y, width, height);
                    ItemModelGenerator.checkTransition(SideDirection.DOWN, sideFaces, sprite, frame, x, y, width, height);
                    ItemModelGenerator.checkTransition(SideDirection.LEFT, sideFaces, sprite, frame, x, y, width, height);
                    ItemModelGenerator.checkTransition(SideDirection.RIGHT, sideFaces, sprite, frame, x, y, width, height);
                }
            }
        });
        return sideFaces;
    }

    private static void checkTransition(SideDirection facing, Set<SideFace> sideFaces, SpriteContents sprite, int frame, int x, int y, int width, int height) {
        if (ItemModelGenerator.isTransparent(sprite, frame, x - facing.direction.getStepX(), y - facing.direction.getStepY(), width, height)) {
            sideFaces.add(new SideFace(facing, x, y));
        }
    }

    private static boolean isTransparent(SpriteContents sprite, int frame, int x, int y, int width, int height) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return true;
        }
        return sprite.isTransparent(frame, x, y);
    }

    @Environment(value=EnvType.CLIENT)
    private record ItemLayerKey(Material.Baked material, ModelState modelState, int layerIndex) implements ModelBaker.SharedOperationKey<QuadCollection>
    {
        @Override
        public QuadCollection compute(ModelBaker modelBakery) {
            QuadCollection.Builder builder = new QuadCollection.Builder();
            BakedQuad.MaterialInfo materialInfo = modelBakery.interner().materialInfo(BakedQuad.MaterialInfo.of(this.material, this.material.sprite().transparency(), this.layerIndex, true, 0));
            ItemModelGenerator.bakeExtrudedSprite(builder, modelBakery.interner(), this.modelState, materialInfo);
            return builder.build();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record SideFace(SideDirection facing, int x, int y) {
    }

    @Environment(value=EnvType.CLIENT)
    private static enum SideDirection {
        UP(Direction.UP),
        DOWN(Direction.DOWN),
        LEFT(Direction.EAST),
        RIGHT(Direction.WEST);

        private final Direction direction;

        private SideDirection(Direction direction) {
            this.direction = direction;
        }

        public Direction getDirection() {
            return this.direction;
        }

        private boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }
}

