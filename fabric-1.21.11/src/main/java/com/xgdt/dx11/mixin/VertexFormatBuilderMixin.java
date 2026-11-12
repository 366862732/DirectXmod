package com.xgdt.dx11.mixin;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.xgdt.dx11.config.Dx11Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Aligns the line-width vertex element to 4 bytes when the D3D11 backend is
 * enabled.
 *
 * <p>Vanilla's {@code POSITION_COLOR_NORMAL_LINE_WIDTH} packs a 4-byte float
 * directly after the 3-byte normal, so {@code LineWidth} ends up at byte
 * offset 19 and the vertex stride is 23.  D3D11 requires R32 elements to be
 * 4-byte aligned (D3D11.3 functional spec, 4.4.6); violating that rule is
 * undefined behavior.  Inserting one padding byte before the line width makes
 * the element offset 20 and the stride 24.  {@code BufferBuilder} writes
 * through {@link VertexFormat#getOffset}, so the written data and the input
 * layout remain consistent.</p>
 *
 * <p>The padding is only applied when the D3D11 backend is actually active,
 * leaving the vanilla OpenGL path byte-for-byte unchanged.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(value = VertexFormat.Builder.class, remap = false)
public class VertexFormatBuilderMixin {
    // VertexFormat$Builder is not obfuscated (it lives under com.mojang.blaze3d),
    // so the class, its fields and its methods keep their names in the
    // intermediary runtime jar.  Disabling remapping is required because the
    // Mixin annotation processor has no obfuscation mapping for this class.
    @Shadow(remap = false)
    private int offset;

    @Inject(method = "add", at = @At("HEAD"), remap = false)
    private void gl4dx11$alignLineWidth(
        String name,
        VertexFormatElement element,
        CallbackInfoReturnable<VertexFormat.Builder> cir) {
        if (!Dx11Config.shouldUseDx11()) {
            return;
        }
        if (element == VertexFormatElement.LINE_WIDTH && (this.offset & 3) != 0) {
            this.offset = (this.offset + 3) & ~3;
        }
    }
}
