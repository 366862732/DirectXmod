/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public interface StructureProcessorType {
    public static final Codec<StructureProcessor> SINGLE_CODEC = BuiltInRegistries.STRUCTURE_PROCESSOR.byNameCodec().dispatch("processor_type", StructureProcessor::codec, c -> c);
    public static final Codec<StructureProcessorList> LIST_OBJECT_CODEC = SINGLE_CODEC.listOf().xmap(StructureProcessorList::new, StructureProcessorList::list);
    public static final Codec<StructureProcessorList> DIRECT_CODEC = Codec.withAlternative((Codec)LIST_OBJECT_CODEC.fieldOf("processors").codec(), LIST_OBJECT_CODEC);
    public static final Codec<Holder<StructureProcessorList>> LIST_CODEC = RegistryFileCodec.create(Registries.PROCESSOR_LIST, DIRECT_CODEC);
}

