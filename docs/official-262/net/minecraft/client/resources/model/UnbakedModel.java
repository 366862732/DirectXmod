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
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface UnbakedModel {
    public static final String PARTICLE_TEXTURE_REFERENCE = "particle";

    default public @Nullable Boolean ambientOcclusion() {
        return null;
    }

    default public @Nullable GuiLight guiLight() {
        return null;
    }

    default public @Nullable ItemTransforms transforms() {
        return null;
    }

    default public TextureSlots.Data textureSlots() {
        return TextureSlots.Data.EMPTY;
    }

    default public @Nullable UnbakedGeometry geometry() {
        return null;
    }

    default public @Nullable Identifier parent() {
        return null;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum GuiLight {
        FRONT("front"),
        SIDE("side");

        private final String name;

        private GuiLight(String name) {
            this.name = name;
        }

        public static GuiLight getByName(String name) {
            for (GuiLight target : GuiLight.values()) {
                if (!target.name.equals(name)) continue;
                return target;
            }
            throw new IllegalArgumentException("Invalid gui light: " + name);
        }

        public boolean lightLikeBlock() {
            return this == SIDE;
        }
    }
}

