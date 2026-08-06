/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerDataHolderRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;

@Environment(value=EnvType.CLIENT)
public class VillagerProfessionLayer<S extends LivingEntityRenderState, M extends EntityModel<S>>
extends RenderLayer<S, M> {
    private static final Int2ObjectMap<Identifier> LEVEL_LOCATIONS = (Int2ObjectMap)Util.make(new Int2ObjectOpenHashMap(), map -> {
        map.put(1, (Object)Identifier.withDefaultNamespace("stone"));
        map.put(2, (Object)Identifier.withDefaultNamespace("iron"));
        map.put(3, (Object)Identifier.withDefaultNamespace("gold"));
        map.put(4, (Object)Identifier.withDefaultNamespace("emerald"));
        map.put(5, (Object)Identifier.withDefaultNamespace("diamond"));
    });
    private final Object2ObjectMap<ResourceKey<VillagerType>, VillagerMetadataSection.Hat> typeHatCache = new Object2ObjectOpenHashMap();
    private final Object2ObjectMap<ResourceKey<VillagerProfession>, VillagerMetadataSection.Hat> professionHatCache = new Object2ObjectOpenHashMap();
    private final ResourceManager resourceManager;
    private final String path;
    private final M noHatModel;
    private final M noHatBabyModel;

    public VillagerProfessionLayer(RenderLayerParent<S, M> renderer, ResourceManager resourceManager, String path, M noHatModel, M noHatBabyModel) {
        super(renderer);
        this.resourceManager = resourceManager;
        this.path = path;
        this.noHatModel = noHatModel;
        this.noHatBabyModel = noHatBabyModel;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
        if (((LivingEntityRenderState)state).isInvisible) {
            return;
        }
        VillagerData villagerData = ((VillagerDataHolderRenderState)state).getVillagerData();
        if (villagerData == null) {
            return;
        }
        Holder<VillagerType> type = villagerData.type();
        Holder<VillagerProfession> profession = villagerData.profession();
        VillagerMetadataSection.Hat typeHat = this.getHatData(this.typeHatCache, "type", type);
        VillagerMetadataSection.Hat professionHat = this.getHatData(this.professionHatCache, "profession", profession);
        Object model = this.getParentModel();
        Identifier typeTexture = this.getIdentifier(((LivingEntityRenderState)state).isBaby ? "baby" : "type", type);
        boolean typeHatVisible = professionHat == VillagerMetadataSection.Hat.NONE || professionHat == VillagerMetadataSection.Hat.PARTIAL && typeHat != VillagerMetadataSection.Hat.FULL;
        M noHatModel = ((LivingEntityRenderState)state).isBaby ? this.noHatBabyModel : this.noHatModel;
        VillagerProfessionLayer.renderColoredCutoutModel(typeHatVisible ? model : noHatModel, typeTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
        if (!profession.is(VillagerProfession.NONE) && !((LivingEntityRenderState)state).isBaby) {
            Identifier professionTexture = this.getIdentifier("profession", profession);
            VillagerProfessionLayer.renderColoredCutoutModel(model, professionTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 2);
            if (!profession.is(VillagerProfession.NITWIT)) {
                Identifier professionLevelTexture = this.getIdentifier("profession_level", (Identifier)LEVEL_LOCATIONS.get(Mth.clamp(villagerData.level(), 1, LEVEL_LOCATIONS.size())));
                VillagerProfessionLayer.renderColoredCutoutModel(model, professionLevelTexture, poseStack, submitNodeCollector, lightCoords, state, -1, 3);
            }
        }
    }

    private Identifier getIdentifier(String type, Identifier key) {
        return key.withPath(keyPath -> "textures/entity/" + this.path + "/" + type + "/" + keyPath + ".png");
    }

    private Identifier getIdentifier(String type, Holder<?> holder) {
        return holder.unwrapKey().map(k -> this.getIdentifier(type, k.identifier())).orElse(MissingTextureAtlasSprite.getLocation());
    }

    public <K> VillagerMetadataSection.Hat getHatData(Object2ObjectMap<ResourceKey<K>, VillagerMetadataSection.Hat> cache, String name, Holder<K> holder) {
        ResourceKey key = holder.unwrapKey().orElse(null);
        if (key == null) {
            return VillagerMetadataSection.Hat.NONE;
        }
        return (VillagerMetadataSection.Hat)cache.computeIfAbsent((Object)key, k -> this.resourceManager.getResource(this.getIdentifier(name, key.identifier())).flatMap(resource -> {
            try {
                return resource.metadata().getSection(VillagerMetadataSection.TYPE).map(VillagerMetadataSection::hat);
            }
            catch (IOException ignored) {
                return Optional.empty();
            }
        }).orElse(VillagerMetadataSection.Hat.NONE));
    }
}

