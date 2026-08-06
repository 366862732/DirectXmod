/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface StructureTags {
    public static final TagKey<Structure> EYE_OF_ENDER_LOCATED = StructureTags.create("eye_of_ender_located");
    public static final TagKey<Structure> DOLPHIN_LOCATED = StructureTags.create("dolphin_located");
    public static final TagKey<Structure> ON_WOODLAND_EXPLORER_MAPS = StructureTags.create("on_woodland_explorer_maps");
    public static final TagKey<Structure> ON_OCEAN_EXPLORER_MAPS = StructureTags.create("on_ocean_explorer_maps");
    public static final TagKey<Structure> ON_SAVANNA_VILLAGE_MAPS = StructureTags.create("on_savanna_village_maps");
    public static final TagKey<Structure> ON_DESERT_VILLAGE_MAPS = StructureTags.create("on_desert_village_maps");
    public static final TagKey<Structure> ON_PLAINS_VILLAGE_MAPS = StructureTags.create("on_plains_village_maps");
    public static final TagKey<Structure> ON_TAIGA_VILLAGE_MAPS = StructureTags.create("on_taiga_village_maps");
    public static final TagKey<Structure> ON_SNOWY_VILLAGE_MAPS = StructureTags.create("on_snowy_village_maps");
    public static final TagKey<Structure> ON_JUNGLE_EXPLORER_MAPS = StructureTags.create("on_jungle_explorer_maps");
    public static final TagKey<Structure> ON_SWAMP_EXPLORER_MAPS = StructureTags.create("on_swamp_explorer_maps");
    public static final TagKey<Structure> ON_TREASURE_MAPS = StructureTags.create("on_treasure_maps");
    public static final TagKey<Structure> ON_TRIAL_CHAMBERS_MAPS = StructureTags.create("on_trial_chambers_maps");
    public static final TagKey<Structure> CATS_SPAWN_IN = StructureTags.create("cats_spawn_in");
    public static final TagKey<Structure> CATS_SPAWN_AS_BLACK = StructureTags.create("cats_spawn_as_black");
    public static final TagKey<Structure> VILLAGE = StructureTags.create("village");
    public static final TagKey<Structure> MINESHAFT = StructureTags.create("mineshaft");
    public static final TagKey<Structure> SHIPWRECK = StructureTags.create("shipwreck");
    public static final TagKey<Structure> RUINED_PORTAL = StructureTags.create("ruined_portal");
    public static final TagKey<Structure> OCEAN_RUIN = StructureTags.create("ocean_ruin");

    private static TagKey<Structure> create(String name) {
        return TagKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace(name));
    }
}

