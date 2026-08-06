/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record SpriteMapper(Identifier sheet, String prefix) {
    public SpriteId apply(Identifier path) {
        return new SpriteId(this.sheet, path.withPrefix(this.prefix + "/"));
    }

    public SpriteId defaultNamespaceApply(String path) {
        return this.apply(Identifier.withDefaultNamespace(path));
    }
}

