/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;

public class FossilFeatureConfiguration
implements FeatureConfiguration {
    public static final Codec<FossilFeatureConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.listOf().fieldOf("fossil_structures").forGetter(t -> t.fossilStructures), (App)Identifier.CODEC.listOf().fieldOf("overlay_structures").forGetter(t -> t.overlayStructures), (App)StructureProcessorType.LIST_CODEC.fieldOf("fossil_processors").forGetter(t -> t.fossilProcessors), (App)StructureProcessorType.LIST_CODEC.fieldOf("overlay_processors").forGetter(t -> t.overlayProcessors), (App)Codec.intRange((int)0, (int)7).fieldOf("max_empty_corners_allowed").forGetter(t -> t.maxEmptyCornersAllowed)).apply((Applicative)i, FossilFeatureConfiguration::new));
    public final List<Identifier> fossilStructures;
    public final List<Identifier> overlayStructures;
    public final Holder<StructureProcessorList> fossilProcessors;
    public final Holder<StructureProcessorList> overlayProcessors;
    public final int maxEmptyCornersAllowed;

    public FossilFeatureConfiguration(List<Identifier> fossilStructures, List<Identifier> overlayStructures, Holder<StructureProcessorList> fossilProcessors, Holder<StructureProcessorList> overlayProcessors, int maxEmptyCornersAllowed) {
        if (fossilStructures.isEmpty()) {
            throw new IllegalArgumentException("Fossil structure lists need at least one entry");
        }
        if (fossilStructures.size() != overlayStructures.size()) {
            throw new IllegalArgumentException("Fossil structure lists must be equal lengths");
        }
        this.fossilStructures = fossilStructures;
        this.overlayStructures = overlayStructures;
        this.fossilProcessors = fossilProcessors;
        this.overlayProcessors = overlayProcessors;
        this.maxEmptyCornersAllowed = maxEmptyCornersAllowed;
    }
}

