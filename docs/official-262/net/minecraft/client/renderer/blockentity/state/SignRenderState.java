/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.blockentity.state;

import com.mojang.math.Transformation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.SignText;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SignRenderState
extends BlockEntityRenderState {
    public @Nullable SignText frontText;
    public @Nullable SignText backText;
    public int textLineHeight;
    public int maxTextLineWidth;
    public boolean isTextFilteringEnabled;
    public boolean drawOutline;
    public SignTransformations transformations = SignTransformations.IDENTITY;

    @Environment(value=EnvType.CLIENT)
    public record SignTransformations(Transformation frontText, Transformation backText) {
        public static final SignTransformations IDENTITY = new SignTransformations(Transformation.IDENTITY, Transformation.IDENTITY);
    }
}

