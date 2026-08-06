/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.apache.commons.lang3.mutable.MutableObject
 */
package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableObject;

public class StructureTemplatePool {
    private static final int SIZE_UNSET = Integer.MIN_VALUE;
    private static final MutableObject<Codec<Holder<StructureTemplatePool>>> CODEC_REFERENCE = new MutableObject();
    public static final Codec<StructureTemplatePool> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.lazyInitialized(CODEC_REFERENCE).fieldOf("fallback").forGetter(StructureTemplatePool::getFallback), (App)Codec.mapPair((MapCodec)StructurePoolElement.CODEC.fieldOf("element"), (MapCodec)Codec.intRange((int)1, (int)150).fieldOf("weight")).codec().listOf().fieldOf("elements").forGetter(p -> p.rawTemplates)).apply((Applicative)i, StructureTemplatePool::new));
    public static final Codec<Holder<StructureTemplatePool>> CODEC = Util.make(RegistryFileCodec.create(Registries.TEMPLATE_POOL, DIRECT_CODEC), arg_0 -> CODEC_REFERENCE.setValue(arg_0));
    private final List<Pair<StructurePoolElement, Integer>> rawTemplates;
    private final ObjectArrayList<StructurePoolElement> templates;
    private final Holder<StructureTemplatePool> fallback;
    private int maxSize = Integer.MIN_VALUE;

    public StructureTemplatePool(Holder<StructureTemplatePool> fallback, List<Pair<StructurePoolElement, Integer>> templates) {
        this.rawTemplates = templates;
        this.templates = new ObjectArrayList();
        for (Pair<StructurePoolElement, Integer> templateDef : templates) {
            StructurePoolElement element = (StructurePoolElement)templateDef.getFirst();
            for (int i = 0; i < (Integer)templateDef.getSecond(); ++i) {
                this.templates.add((Object)element);
            }
        }
        this.fallback = fallback;
    }

    public StructureTemplatePool(Holder<StructureTemplatePool> fallback, List<Pair<Function<Projection, ? extends StructurePoolElement>, Integer>> templates, Projection projection) {
        this.rawTemplates = Lists.newArrayList();
        this.templates = new ObjectArrayList();
        for (Pair<Function<Projection, ? extends StructurePoolElement>, Integer> templateDef : templates) {
            StructurePoolElement element = (StructurePoolElement)((Function)templateDef.getFirst()).apply(projection);
            this.rawTemplates.add((Pair<StructurePoolElement, Integer>)Pair.of((Object)element, (Object)((Integer)templateDef.getSecond())));
            for (int i = 0; i < (Integer)templateDef.getSecond(); ++i) {
                this.templates.add((Object)element);
            }
        }
        this.fallback = fallback;
    }

    public int getMaxSize(StructureTemplateManager manager) {
        if (this.maxSize == Integer.MIN_VALUE) {
            this.maxSize = this.templates.stream().filter(t -> t != EmptyPoolElement.INSTANCE).mapToInt(t -> t.getBoundingBox(manager, BlockPos.ZERO, Rotation.NONE).getYSpan()).max().orElse(0);
        }
        return this.maxSize;
    }

    @VisibleForTesting
    public List<Pair<StructurePoolElement, Integer>> getTemplates() {
        return this.rawTemplates;
    }

    public Holder<StructureTemplatePool> getFallback() {
        return this.fallback;
    }

    public StructurePoolElement getRandomTemplate(RandomSource random) {
        if (this.templates.isEmpty()) {
            return EmptyPoolElement.INSTANCE;
        }
        return (StructurePoolElement)this.templates.get(random.nextInt(this.templates.size()));
    }

    public List<StructurePoolElement> getShuffledTemplates(RandomSource random) {
        return Util.shuffledCopy(this.templates, random);
    }

    public int size() {
        return this.templates.size();
    }

    public static enum Projection implements StringRepresentable
    {
        TERRAIN_MATCHING("terrain_matching", (ImmutableList<StructureProcessor>)ImmutableList.of((Object)new GravityProcessor(Heightmap.Types.WORLD_SURFACE_WG, -1))),
        RIGID("rigid", (ImmutableList<StructureProcessor>)ImmutableList.of());

        public static final StringRepresentable.EnumCodec<Projection> CODEC;
        private final String name;
        private final ImmutableList<StructureProcessor> processors;

        private Projection(String name, ImmutableList<StructureProcessor> processors) {
            this.name = name;
            this.processors = processors;
        }

        public String getName() {
            return this.name;
        }

        public static Projection byName(String name) {
            return CODEC.byName(name);
        }

        public ImmutableList<StructureProcessor> getProcessors() {
            return this.processors;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Projection::values);
        }
    }
}

