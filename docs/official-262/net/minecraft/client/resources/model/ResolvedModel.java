/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.world.item.ItemDisplayContext;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface ResolvedModel
extends ModelDebugName {
    public static final boolean DEFAULT_AMBIENT_OCCLUSION = true;
    public static final UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

    public UnbakedModel wrapped();

    public @Nullable ResolvedModel parent();

    public static TextureSlots findTopTextureSlots(ResolvedModel top) {
        TextureSlots.Resolver resolver = new TextureSlots.Resolver();
        for (ResolvedModel current = top; current != null; current = current.parent()) {
            resolver.addLast(current.wrapped().textureSlots());
        }
        return resolver.resolve(top);
    }

    default public TextureSlots getTopTextureSlots() {
        return ResolvedModel.findTopTextureSlots(this);
    }

    public static boolean findTopAmbientOcclusion(ResolvedModel current) {
        while (current != null) {
            Boolean hasAmbientOcclusion = current.wrapped().ambientOcclusion();
            if (hasAmbientOcclusion != null) {
                return hasAmbientOcclusion;
            }
            current = current.parent();
        }
        return true;
    }

    default public boolean getTopAmbientOcclusion() {
        return ResolvedModel.findTopAmbientOcclusion(this);
    }

    public static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel current) {
        while (current != null) {
            UnbakedModel.GuiLight guiLight = current.wrapped().guiLight();
            if (guiLight != null) {
                return guiLight;
            }
            current = current.parent();
        }
        return DEFAULT_GUI_LIGHT;
    }

    default public UnbakedModel.GuiLight getTopGuiLight() {
        return ResolvedModel.findTopGuiLight(this);
    }

    public static UnbakedGeometry findTopGeometry(ResolvedModel current) {
        while (current != null) {
            UnbakedGeometry geometry = current.wrapped().geometry();
            if (geometry != null) {
                return geometry;
            }
            current = current.parent();
        }
        return UnbakedGeometry.EMPTY;
    }

    default public UnbakedGeometry getTopGeometry() {
        return ResolvedModel.findTopGeometry(this);
    }

    default public QuadCollection bakeTopGeometry(TextureSlots textureSlots, ModelBaker baker, ModelState state) {
        return this.getTopGeometry().bake(textureSlots, baker, state, this);
    }

    public static Material.Baked resolveParticleMaterial(TextureSlots textureSlots, ModelBaker baker, ModelDebugName resolvedModel) {
        return baker.materials().resolveSlot(textureSlots, "particle", resolvedModel);
    }

    default public Material.Baked resolveParticleMaterial(TextureSlots textureSlots, ModelBaker baker) {
        return ResolvedModel.resolveParticleMaterial(textureSlots, baker, this);
    }

    public static ItemTransform findTopTransform(ResolvedModel current, ItemDisplayContext type) {
        while (current != null) {
            ItemTransform transform;
            ItemTransforms transforms = current.wrapped().transforms();
            if (transforms != null && (transform = transforms.getTransform(type)) != ItemTransform.NO_TRANSFORM) {
                return transform;
            }
            current = current.parent();
        }
        return ItemTransform.NO_TRANSFORM;
    }

    public static ItemTransforms findTopTransforms(ResolvedModel top) {
        ItemTransform thirdPersonLeftHand = ResolvedModel.findTopTransform(top, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform thirdPersonRightHand = ResolvedModel.findTopTransform(top, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform firstPersonLeftHand = ResolvedModel.findTopTransform(top, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform firstPersonRightHand = ResolvedModel.findTopTransform(top, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform head = ResolvedModel.findTopTransform(top, ItemDisplayContext.HEAD);
        ItemTransform gui = ResolvedModel.findTopTransform(top, ItemDisplayContext.GUI);
        ItemTransform ground = ResolvedModel.findTopTransform(top, ItemDisplayContext.GROUND);
        ItemTransform fixed = ResolvedModel.findTopTransform(top, ItemDisplayContext.FIXED);
        ItemTransform fixedFromBottom = ResolvedModel.findTopTransform(top, ItemDisplayContext.ON_SHELF);
        return new ItemTransforms(thirdPersonLeftHand, thirdPersonRightHand, firstPersonLeftHand, firstPersonRightHand, head, gui, ground, fixed, fixedFromBottom);
    }

    default public ItemTransforms getTopTransforms() {
        return ResolvedModel.findTopTransforms(this);
    }
}

