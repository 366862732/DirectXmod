/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class OutputTarget {
    private final String name;
    private final Supplier<@Nullable RenderTarget> renderTargetSupplier;
    public static final OutputTarget MAIN_TARGET = new OutputTarget("main_target", () -> Minecraft.getInstance().gameRenderer.mainRenderTarget());
    public static final OutputTarget OUTLINE_TARGET = new OutputTarget("outline_target", () -> Minecraft.getInstance().levelRenderer.entityOutlineTarget());
    public static final OutputTarget WEATHER_TARGET = new OutputTarget("weather_target", () -> Minecraft.getInstance().levelRenderer.weatherTarget());
    public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget("item_entity_target", () -> Minecraft.getInstance().levelRenderer.itemEntityTarget());

    public OutputTarget(String name, Supplier<@Nullable RenderTarget> renderTargetSupplier) {
        this.name = name;
        this.renderTargetSupplier = renderTargetSupplier;
    }

    public RenderTarget getRenderTarget() {
        RenderTarget preferredTarget = this.renderTargetSupplier.get();
        return preferredTarget != null ? preferredTarget : Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    public String toString() {
        return "OutputTarget[" + this.name + "]";
    }
}

