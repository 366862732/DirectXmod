/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public record EquipmentClientInfo(Map<LayerType, List<Layer>> layers) {
    private static final Codec<List<Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(Layer.CODEC.listOf());
    public static final Codec<EquipmentClientInfo> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.nonEmptyMap(Codec.unboundedMap(LayerType.CODEC, LAYER_LIST_CODEC)).fieldOf("layers").forGetter(EquipmentClientInfo::layers)).apply((Applicative)i, EquipmentClientInfo::new));

    public static Builder builder() {
        return new Builder();
    }

    public List<Layer> getLayers(LayerType type) {
        return this.layers.getOrDefault(type, List.of());
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Map<LayerType, List<Layer>> layersByType = new EnumMap<LayerType, List<Layer>>(LayerType.class);

        private Builder() {
        }

        public Builder addHumanoidLayers(Identifier textureId) {
            return this.addHumanoidLayers(textureId, false);
        }

        public Builder addHumanoidLayers(Identifier textureId, boolean dyeable) {
            this.addLayers(LayerType.HUMANOID_LEGGINGS, Layer.leatherDyeable(textureId, dyeable));
            this.addMainHumanoidLayer(textureId, dyeable);
            return this;
        }

        public Builder addMainHumanoidLayer(Identifier textureId, boolean dyeable) {
            this.addLayers(LayerType.HUMANOID, Layer.leatherDyeable(textureId, dyeable));
            this.addLayers(LayerType.HUMANOID_BABY, Layer.leatherDyeable(textureId, dyeable));
            return this;
        }

        public Builder addLayers(LayerType type, Layer ... layers) {
            Collections.addAll(this.layersByType.computeIfAbsent(type, t -> new ArrayList()), layers);
            return this;
        }

        public EquipmentClientInfo build() {
            return new EquipmentClientInfo((Map)this.layersByType.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, entry -> List.copyOf((Collection)entry.getValue()))));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum LayerType implements StringRepresentable
    {
        HUMANOID("humanoid"),
        HUMANOID_LEGGINGS("humanoid_leggings"),
        HUMANOID_BABY("humanoid_baby"),
        WINGS("wings"),
        WOLF_BODY("wolf_body"),
        HORSE_BODY("horse_body"),
        LLAMA_BODY("llama_body"),
        PIG_SADDLE("pig_saddle"),
        STRIDER_SADDLE("strider_saddle"),
        CAMEL_SADDLE("camel_saddle"),
        CAMEL_HUSK_SADDLE("camel_husk_saddle"),
        HORSE_SADDLE("horse_saddle"),
        DONKEY_SADDLE("donkey_saddle"),
        MULE_SADDLE("mule_saddle"),
        ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
        SKELETON_HORSE_SADDLE("skeleton_horse_saddle"),
        HAPPY_GHAST_BODY("happy_ghast_body"),
        NAUTILUS_SADDLE("nautilus_saddle"),
        NAUTILUS_BODY("nautilus_body");

        public static final Codec<LayerType> CODEC;
        private final String id;

        private LayerType(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public String trimAssetPrefix() {
            return "trims/entity/" + this.id;
        }

        static {
            CODEC = StringRepresentable.fromEnum(LayerType::values);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Layer(Identifier textureId, Optional<Dyeable> dyeable, boolean usePlayerTexture) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("texture").forGetter(Layer::textureId), (App)Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(Layer::dyeable), (App)Codec.BOOL.optionalFieldOf("use_player_texture", (Object)false).forGetter(Layer::usePlayerTexture)).apply((Applicative)i, Layer::new));

        public Layer(Identifier textureId) {
            this(textureId, Optional.empty(), false);
        }

        public static Layer leatherDyeable(Identifier textureId, boolean dyeable) {
            return new Layer(textureId, dyeable ? Optional.of(new Dyeable(Optional.of(-6265536))) : Optional.empty(), false);
        }

        public static Layer onlyIfDyed(Identifier textureId, boolean dyeable) {
            return new Layer(textureId, dyeable ? Optional.of(new Dyeable(Optional.empty())) : Optional.empty(), false);
        }

        public Identifier getTextureLocation(LayerType type) {
            return this.textureId.withPath(path -> "textures/entity/equipment/" + type.getSerializedName() + "/" + path + ".png");
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Dyeable(Optional<Integer> colorWhenUndyed) {
        public static final Codec<Dyeable> CODEC = RecordCodecBuilder.create(i -> i.group((App)ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(Dyeable::colorWhenUndyed)).apply((Applicative)i, Dyeable::new));
    }
}

