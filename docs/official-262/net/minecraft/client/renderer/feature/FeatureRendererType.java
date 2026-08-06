/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.feature;

import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

@Environment(value=EnvType.CLIENT)
public record FeatureRendererType<Submit extends SubmitNode>(int id, String name) {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    public static <Submit extends SubmitNode> FeatureRendererType<Submit> create(String name) {
        return new FeatureRendererType<Submit>(NEXT_ID.getAndIncrement(), name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}

