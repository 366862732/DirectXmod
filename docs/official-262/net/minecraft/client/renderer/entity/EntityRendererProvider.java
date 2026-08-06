/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface EntityRendererProvider<T extends Entity> {
    public EntityRenderer<T, ?> create(Context var1);

    @Environment(value=EnvType.CLIENT)
    public static class Context {
        private final EntityRenderDispatcher entityRenderDispatcher;
        private final BlockModelResolver blockModelResolver;
        private final ItemModelResolver itemModelResolver;
        private final MapRenderer mapRenderer;
        private final ResourceManager resourceManager;
        private final EntityModelSet modelSet;
        private final EquipmentAssetManager equipmentAssets;
        private final Font font;
        private final EquipmentLayerRenderer equipmentRenderer;
        private final AtlasManager atlasManager;
        private final PlayerSkinRenderCache playerSkinRenderCache;

        public Context(EntityRenderDispatcher entityRenderDispatcher, BlockModelResolver blockModelResolver, ItemModelResolver itemModelResolver, MapRenderer mapRenderer, ResourceManager resourceManager, EntityModelSet modelSet, EquipmentAssetManager equipmentAssets, AtlasManager atlasManager, Font font, PlayerSkinRenderCache playerSkinRenderCache) {
            this.entityRenderDispatcher = entityRenderDispatcher;
            this.blockModelResolver = blockModelResolver;
            this.itemModelResolver = itemModelResolver;
            this.mapRenderer = mapRenderer;
            this.resourceManager = resourceManager;
            this.modelSet = modelSet;
            this.equipmentAssets = equipmentAssets;
            this.font = font;
            this.atlasManager = atlasManager;
            this.playerSkinRenderCache = playerSkinRenderCache;
            this.equipmentRenderer = new EquipmentLayerRenderer(equipmentAssets, atlasManager.getAtlasOrThrow(AtlasIds.ARMOR_TRIMS));
        }

        public EntityRenderDispatcher getEntityRenderDispatcher() {
            return this.entityRenderDispatcher;
        }

        public BlockModelResolver getBlockModelResolver() {
            return this.blockModelResolver;
        }

        public ItemModelResolver getItemModelResolver() {
            return this.itemModelResolver;
        }

        public MapRenderer getMapRenderer() {
            return this.mapRenderer;
        }

        public ResourceManager getResourceManager() {
            return this.resourceManager;
        }

        public EntityModelSet getModelSet() {
            return this.modelSet;
        }

        public EquipmentAssetManager getEquipmentAssets() {
            return this.equipmentAssets;
        }

        public EquipmentLayerRenderer getEquipmentRenderer() {
            return this.equipmentRenderer;
        }

        public SpriteGetter getSprites() {
            return this.atlasManager;
        }

        public TextureAtlas getAtlas(Identifier sheet) {
            return this.atlasManager.getAtlasOrThrow(sheet);
        }

        public ModelPart bakeLayer(ModelLayerLocation id) {
            return this.modelSet.bakeLayer(id);
        }

        public Font getFont() {
            return this.font;
        }

        public PlayerSkinRenderCache getPlayerSkinRenderCache() {
            return this.playerSkinRenderCache;
        }
    }
}

