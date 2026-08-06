/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerAdvancementManager
extends SimpleJsonResourceReloadListener<Advancement> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<Identifier, AdvancementHolder> advancements = Map.of();
    private AdvancementTree tree = new AdvancementTree();
    private final HolderLookup.Provider registries;

    public ServerAdvancementManager(HolderLookup.Provider registries) {
        super(registries, Advancement.CODEC, Registries.ADVANCEMENT);
        this.registries = registries;
    }

    @Override
    protected void apply(Map<Identifier, Advancement> preparations, ResourceManager manager, ProfilerFiller profiler) {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        preparations.forEach((id, advancement) -> {
            this.validate((Identifier)id, (Advancement)advancement);
            builder.put(id, (Object)new AdvancementHolder((Identifier)id, (Advancement)advancement));
        });
        this.advancements = builder.buildOrThrow();
        AdvancementTree tree = new AdvancementTree();
        tree.addAll(this.advancements.values());
        for (AdvancementNode root : tree.roots()) {
            if (!root.holder().value().display().isPresent()) continue;
            TreeNodePosition.run(root);
        }
        this.tree = tree;
    }

    private void validate(Identifier id, Advancement advancement) {
        ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
        advancement.validate(problemCollector, this.registries);
        if (!problemCollector.isEmpty()) {
            LOGGER.warn("Found validation problems in advancement {}: \n{}", (Object)id, (Object)problemCollector.getReport());
        }
    }

    public @Nullable AdvancementHolder get(Identifier id) {
        return this.advancements.get(id);
    }

    public AdvancementTree tree() {
        return this.tree;
    }

    public Collection<AdvancementHolder> getAllAdvancements() {
        return this.advancements.values();
    }
}

