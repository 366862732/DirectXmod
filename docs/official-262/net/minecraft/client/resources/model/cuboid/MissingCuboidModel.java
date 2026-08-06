/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package net.minecraft.client.resources.model.cuboid;

import com.mojang.math.Quadrant;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Environment(value=EnvType.CLIENT)
public class MissingCuboidModel {
    private static final String TEXTURE_SLOT = "missingno";
    public static final Identifier LOCATION = Identifier.withDefaultNamespace("builtin/missing");

    public static UnbakedModel missingModel() {
        CuboidFace.UVs fullFaceUv = new CuboidFace.UVs(0.0f, 0.0f, 16.0f, 16.0f);
        Map<Direction, CuboidFace> faces = Util.makeEnumMap(Direction.class, direction -> new CuboidFace((Direction)direction, -1, TEXTURE_SLOT, fullFaceUv, Quadrant.R0));
        CuboidModelElement cube = new CuboidModelElement((Vector3fc)new Vector3f(0.0f, 0.0f, 0.0f), (Vector3fc)new Vector3f(16.0f, 16.0f, 16.0f), faces);
        return new CuboidModel(new UnbakedCuboidGeometry(List.of(cube)), null, null, ItemTransforms.NO_TRANSFORMS, new TextureSlots.Data.Builder().addReference("particle", TEXTURE_SLOT).addTexture(TEXTURE_SLOT, new Material(MissingTextureAtlasSprite.getLocation())).build(), null);
    }
}

