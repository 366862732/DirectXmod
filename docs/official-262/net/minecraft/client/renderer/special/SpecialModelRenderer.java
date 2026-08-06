/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public interface SpecialModelRenderer<T> {
    public void submit(@Nullable T var1, PoseStack var2, SubmitNodeCollector var3, int var4, int var5, boolean var6, int var7);

    public void getExtents(Consumer<Vector3fc> var1);

    public @Nullable T extractArgument(ItemStack var1);

    @Environment(value=EnvType.CLIENT)
    public static interface BakingContext {
        public EntityModelSet entityModelSet();

        public SpriteGetter sprites();

        public PlayerSkinRenderCache playerSkinRenderCache();
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Unbaked<T> {
        public @Nullable SpecialModelRenderer<T> bake(BakingContext var1);

        public MapCodec<? extends Unbaked<T>> type();
    }
}

