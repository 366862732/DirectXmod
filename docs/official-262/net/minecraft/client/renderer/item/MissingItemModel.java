/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Suppliers
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class MissingItemModel
implements ItemModel {
    private final List<BakedQuad> quads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transform;

    public MissingItemModel(List<BakedQuad> quads, ModelRenderProperties properties) {
        this(quads, (Supplier<Vector3fc[]>)Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads)), properties, (Matrix4fc)new Matrix4f());
    }

    private MissingItemModel(List<BakedQuad> quads, Supplier<Vector3fc[]> extents, ModelRenderProperties properties, Matrix4fc transform) {
        this.quads = quads;
        this.extents = extents;
        this.properties = properties;
        this.transform = transform;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        this.properties.applyToLayer(layer, displayContext);
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transform);
        layer.prepareQuadList().addAll(this.quads);
    }

    public MissingItemModel withTransform(Matrix4fc transform) {
        if (transform.equals((Object)this.transform)) {
            return this;
        }
        return new MissingItemModel(this.quads, this.extents, this.properties, transform);
    }
}

