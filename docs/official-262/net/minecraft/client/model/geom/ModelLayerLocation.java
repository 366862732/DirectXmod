/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.model.geom;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public record ModelLayerLocation(Identifier model, String layer) {
    @Override
    public String toString() {
        return String.valueOf(this.model) + "#" + this.layer;
    }
}

