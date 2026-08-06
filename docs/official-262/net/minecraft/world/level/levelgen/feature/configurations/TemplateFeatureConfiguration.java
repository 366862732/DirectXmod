/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record TemplateFeatureConfiguration(WeightedList<TemplateEntry> templates) implements FeatureConfiguration
{
    public static final Codec<TemplateFeatureConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group((App)WeightedList.codec(TemplateEntry.CODEC).fieldOf("templates").forGetter(TemplateFeatureConfiguration::templates)).apply((Applicative)i, TemplateFeatureConfiguration::new));

    public record TemplateEntry(Identifier template, List<Rotation> rotations) {
        public static final Codec<TemplateEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)Identifier.CODEC.fieldOf("id").forGetter(TemplateEntry::template), (App)Rotation.CODEC.listOf().optionalFieldOf("rotations", List.of(Rotation.values())).forGetter(TemplateEntry::rotations)).apply((Applicative)i, TemplateEntry::new));

        public static TemplateEntry of(Identifier template) {
            return new TemplateEntry(template, List.of(Rotation.values()));
        }
    }
}

