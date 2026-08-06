/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.model.sprite;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record SpriteId(Identifier atlasLocation, Identifier texture) {
    public RenderType renderType(Function<Identifier, RenderType> renderType) {
        return renderType.apply(this.atlasLocation);
    }
}

