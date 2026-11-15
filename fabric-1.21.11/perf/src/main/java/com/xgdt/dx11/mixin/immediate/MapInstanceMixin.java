package com.xgdt.dx11.mixin.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xgdt.dx11.perf.immediate.ImmediateStats;
import com.xgdt.dx11.perf.immediate.map.MapAtlas;
import com.xgdt.dx11.perf.immediate.map.MapAtlasManagerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Redirects a map instance's texture and uploads to its atlas tile.
 *
 * <p>Vanilla constructs a 128x128 {@link DynamicTexture} and registers it per
 * map.  Atlas-backed instances still let that constructor run (so all field
 * initialisation stays vanilla), but the registration is diverted to the
 * atlas and the throw-away texture is closed immediately.  The instance's
 * {@code location} then points at the shared atlas, which is what both the GUI
 * and the world map renderers use to bind the texture.</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.resources.MapTextureManager$MapInstance")
public abstract class MapInstanceMixin {
    @Shadow
    private MapItemSavedData data;

    @Shadow
    private boolean requiresUpload;

    @Shadow
    @Final
    @Mutable
    Identifier location;

    @Unique
    private MapAtlas dx11$atlas;

    @Unique
    private int dx11$atlasX;

    @Unique
    private int dx11$atlasY;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;<init>(Ljava/util/function/Supplier;IIZ)V"))
    private void dx11$allocateAtlasTile(
        MapTextureManager manager, int id, MapItemSavedData mapData, CallbackInfo ci) {
        if (!(manager instanceof MapAtlasManagerAccess access)) {
            return;
        }
        int slot = access.dx11$allocateAtlasSlot(id);
        if (slot == -1) {
            return;
        }
        MapAtlas atlas = access.dx11$atlas(MapAtlas.atlasId(slot));
        if (atlas == null) {
            return;
        }
        this.dx11$atlas = atlas;
        this.dx11$atlasX = MapAtlas.column(slot) * MapAtlas.MAP_SIZE;
        this.dx11$atlasY = MapAtlas.row(slot) * MapAtlas.MAP_SIZE;
    }

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;register(Lnet/minecraft/resources/Identifier;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V"))
    private void dx11$registerAtlasInstead(
        TextureManager manager,
        Identifier path,
        AbstractTexture texture,
        Operation<Void> original) {
        if (this.dx11$atlas != null) {
            this.location = this.dx11$atlas.identifier();
            if (texture instanceof DynamicTexture dynamic) {
                dynamic.close();
            }
            return;
        }
        original.call(manager, path, texture);
    }

    @Inject(method = "updateTextureIfNeeded", at = @At("HEAD"), cancellable = true)
    private void dx11$uploadAtlasTile(CallbackInfo ci) {
        if (this.dx11$atlas == null || !this.requiresUpload) {
            return;
        }
        ci.cancel();
        if (this.data.colors != null) {
            this.dx11$atlas.uploadTile(this.dx11$atlasX, this.dx11$atlasY, this.data.colors);
            ImmediateStats.onMapUpload();
        }
        this.requiresUpload = false;
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void dx11$keepAtlasOpen(CallbackInfo ci) {
        if (this.dx11$atlas != null) {
            // The shared atlas is released by MapTextureManager.resetData.
            ci.cancel();
        }
    }
}
