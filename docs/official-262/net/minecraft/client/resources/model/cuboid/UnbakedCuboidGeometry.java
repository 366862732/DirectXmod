/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 */
package net.minecraft.client.resources.model.cuboid;

import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public record UnbakedCuboidGeometry(List<CuboidModelElement> elements) implements UnbakedGeometry
{
    @Override
    public QuadCollection bake(TextureSlots textures, ModelBaker modelBaker, ModelState modelState, ModelDebugName name) {
        return UnbakedCuboidGeometry.bake(this.elements, textures, modelBaker, modelState, name);
    }

    public static QuadCollection bake(List<CuboidModelElement> elements, TextureSlots textures, ModelBaker modelBaker, ModelState modelState, ModelDebugName name) {
        QuadCollection.Builder builder = new QuadCollection.Builder();
        for (CuboidModelElement element : elements) {
            boolean drawXFaces = true;
            boolean drawYFaces = true;
            boolean drawZFaces = true;
            Vector3fc from = element.from();
            Vector3fc to = element.to();
            if (from.x() == to.x()) {
                drawYFaces = false;
                drawZFaces = false;
            }
            if (from.y() == to.y()) {
                drawXFaces = false;
                drawZFaces = false;
            }
            if (from.z() == to.z()) {
                drawXFaces = false;
                drawYFaces = false;
            }
            if (!drawXFaces && !drawYFaces && !drawZFaces) continue;
            for (Map.Entry<Direction, CuboidFace> entry : element.faces().entrySet()) {
                boolean shouldDrawFace;
                Direction facing = entry.getKey();
                CuboidFace face = entry.getValue();
                if (!(shouldDrawFace = (switch (facing.getAxis()) {
                    default -> throw new MatchException(null, null);
                    case Direction.Axis.X -> drawXFaces;
                    case Direction.Axis.Y -> drawYFaces;
                    case Direction.Axis.Z -> drawZFaces;
                }))) continue;
                Material.Baked material = modelBaker.materials().resolveSlot(textures, face.texture(), name);
                BakedQuad quad = FaceBakery.bakeQuad(modelBaker, from, to, face, material, facing, modelState, element.rotation(), element.shade(), element.lightEmission());
                if (face.cullForDirection() == null) {
                    builder.addUnculledFace(quad);
                    continue;
                }
                builder.addCulledFace(Direction.rotate(modelState.transformation().getMatrix(), face.cullForDirection()), quad);
            }
        }
        return builder.build();
    }
}

