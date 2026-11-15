package com.xgdt.dx11.mixin.immediate;

import com.xgdt.dx11.perf.immediate.sign.SignTextCacheable;
import com.xgdt.dx11.perf.immediate.sign.SignTextKey;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.SignText;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds cacheability information to {@link SignText} without touching its
 * {@code equals}/{@code hashCode} contract.
 *
 * <p>Obfuscated text animates every frame, so it is never rasterised into the
 * atlas.  The check runs once per computed message array and is invalidated
 * whenever {@code renderMessages} is rebuilt (for example when the filtered
 * view is enabled).  The content key is cached on the instance because all
 * {@code SignText} setters return a new object.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(SignText.class)
public abstract class SignTextMixin implements SignTextCacheable {
    @Shadow
    @Final
    private Component[] messages;

    @Shadow
    @Final
    private Component[] filteredMessages;

    @Shadow
    @Final
    private DyeColor color;

    @Shadow
    @Final
    private boolean hasGlowingText;

    @Unique
    private boolean dx11$shouldCache;

    @Unique
    private boolean dx11$checkedCache;

    @Unique
    private SignTextKey dx11$key;

    @Unique
    private boolean dx11$keyFiltering;

    @Unique
    private int dx11$keyLineHeight;

    @Unique
    private int dx11$keyMaxLineWidth;

    @Inject(method = "getRenderMessages", at = @At("RETURN"))
    private void dx11$inspectRenderMessages(
        boolean filtering,
        Function<Component, FormattedCharSequence> splitter,
        CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        if (this.dx11$checkedCache) {
            return;
        }
        this.dx11$checkedCache = true;

        FormattedCharSequence[] lines = cir.getReturnValue();
        if (lines == null) {
            this.dx11$shouldCache = false;
            return;
        }
        for (FormattedCharSequence line : lines) {
            if (line == null || !line.accept((index, style, codePoint) -> !style.isObfuscated())) {
                this.dx11$shouldCache = false;
                return;
            }
        }
        this.dx11$shouldCache = true;
    }

    @Inject(
        method = "getRenderMessages",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/block/entity/SignText;renderMessages:[Lnet/minecraft/util/FormattedCharSequence;",
            opcode = Opcodes.PUTFIELD))
    private void dx11$invalidateCache(
        boolean filtering,
        Function<Component, FormattedCharSequence> splitter,
        CallbackInfoReturnable<FormattedCharSequence[]> cir) {
        this.dx11$shouldCache = false;
        this.dx11$checkedCache = false;
        this.dx11$key = null;
    }

    @Override
    public boolean dx11$shouldCache() {
        return this.dx11$shouldCache;
    }

    @Override
    public void dx11$setShouldCache(boolean shouldCache) {
        this.dx11$shouldCache = shouldCache;
    }

    @Override
    public SignTextKey dx11$signTextKey(boolean filtering, int lineHeight, int maxLineWidth) {
        SignTextKey key = this.dx11$key;
        if (key == null
            || this.dx11$keyFiltering != filtering
            || this.dx11$keyLineHeight != lineHeight
            || this.dx11$keyMaxLineWidth != maxLineWidth) {
            key = new SignTextKey(
                this.messages,
                this.filteredMessages,
                this.color,
                this.hasGlowingText,
                filtering,
                lineHeight,
                maxLineWidth);
            this.dx11$key = key;
            this.dx11$keyFiltering = filtering;
            this.dx11$keyLineHeight = lineHeight;
            this.dx11$keyMaxLineWidth = maxLineWidth;
        }
        return key;
    }
}
