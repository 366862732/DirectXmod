/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.storage.loot.functions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

public class ExplorationMapFunction
extends LootItemConditionalFunction {
    public static final TagKey<Structure> DEFAULT_DESTINATION = StructureTags.ON_TREASURE_MAPS;
    public static final Holder<MapDecorationType> DEFAULT_DECORATION = MapDecorationTypes.WOODLAND_MANSION;
    public static final byte DEFAULT_ZOOM = 2;
    public static final int DEFAULT_SEARCH_RADIUS = 50;
    public static final boolean DEFAULT_SKIP_EXISTING = true;
    public static final MapCodec<ExplorationMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> ExplorationMapFunction.commonFields(i).and(i.group((App)TagKey.codec(Registries.STRUCTURE).optionalFieldOf("destination", DEFAULT_DESTINATION).forGetter(f -> f.destination), (App)MapDecorationType.CODEC.optionalFieldOf("decoration", DEFAULT_DECORATION).forGetter(f -> f.mapDecoration), (App)Codec.BYTE.optionalFieldOf("zoom", (Object)2).forGetter(f -> f.zoom), (App)Codec.INT.optionalFieldOf("search_radius", (Object)50).forGetter(f -> f.searchRadius), (App)Codec.BOOL.optionalFieldOf("skip_existing_chunks", (Object)true).forGetter(f -> f.skipKnownStructures))).apply((Applicative)i, ExplorationMapFunction::new));
    private final TagKey<Structure> destination;
    private final Holder<MapDecorationType> mapDecoration;
    private final byte zoom;
    private final int searchRadius;
    private final boolean skipKnownStructures;

    private ExplorationMapFunction(List<LootItemCondition> predicates, TagKey<Structure> destination, Holder<MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures) {
        super(predicates);
        this.destination = destination;
        this.mapDecoration = mapDecoration;
        this.zoom = zoom;
        this.searchRadius = searchRadius;
        this.skipKnownStructures = skipKnownStructures;
    }

    public MapCodec<ExplorationMapFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ORIGIN);
    }

    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        ServerLevel level;
        BlockPos nearestMapStructure;
        if (!itemStack.is(Items.MAP)) {
            return itemStack;
        }
        Vec3 lootPos = context.getOptionalParameter(LootContextParams.ORIGIN);
        if (lootPos != null && (nearestMapStructure = (level = context.getLevel()).findNearestMapStructure(this.destination, BlockPos.containing(lootPos), this.searchRadius, this.skipKnownStructures)) != null) {
            ItemStack map = MapItem.create(level, nearestMapStructure.getX(), nearestMapStructure.getZ(), this.zoom, true, true);
            MapItem.renderBiomePreviewMap(level, map);
            MapItemSavedData.addTargetDecoration(map, nearestMapStructure, "+", this.mapDecoration);
            return map;
        }
        return itemStack;
    }

    public static Builder makeExplorationMap() {
        return new Builder();
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private TagKey<Structure> destination = DEFAULT_DESTINATION;
        private Holder<MapDecorationType> mapDecoration = DEFAULT_DECORATION;
        private byte zoom = (byte)2;
        private int searchRadius = 50;
        private boolean skipKnownStructures = true;

        @Override
        protected Builder getThis() {
            return this;
        }

        public Builder setDestination(TagKey<Structure> destination) {
            this.destination = destination;
            return this;
        }

        public Builder setMapDecoration(Holder<MapDecorationType> mapDecoration) {
            this.mapDecoration = mapDecoration;
            return this;
        }

        public Builder setZoom(byte zoom) {
            this.zoom = zoom;
            return this;
        }

        public Builder setSearchRadius(int searchRadius) {
            this.searchRadius = searchRadius;
            return this;
        }

        public Builder setSkipKnownStructures(boolean skipKnownStructures) {
            this.skipKnownStructures = skipKnownStructures;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
        }
    }
}

