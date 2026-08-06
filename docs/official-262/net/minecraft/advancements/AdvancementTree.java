/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.advancements;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AdvancementTree {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<Identifier, AdvancementNode> nodes = new Object2ObjectOpenHashMap();
    private final Set<AdvancementNode> roots = new ObjectLinkedOpenHashSet();
    private final Set<AdvancementNode> tasks = new ObjectLinkedOpenHashSet();
    private @Nullable Listener listener;

    private void remove(AdvancementNode node) {
        for (AdvancementNode child : node.children()) {
            this.remove(child);
        }
        LOGGER.info("Forgot about advancement {}", (Object)node.holder());
        this.nodes.remove(node.holder().id());
        if (node.parent() == null) {
            this.roots.remove(node);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementRoot(node);
            }
        } else {
            this.tasks.remove(node);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementTask(node);
            }
        }
    }

    public void remove(Set<Identifier> ids) {
        for (Identifier id : ids) {
            AdvancementNode advancement = this.nodes.get(id);
            if (advancement == null) {
                LOGGER.warn("Told to remove advancement {} but I don't know what that is", (Object)id);
                continue;
            }
            this.remove(advancement);
        }
    }

    public void addAll(Collection<AdvancementHolder> advancements) {
        ArrayList<AdvancementHolder> advancementsToAdd = new ArrayList<AdvancementHolder>(advancements);
        while (!advancementsToAdd.isEmpty()) {
            if (advancementsToAdd.removeIf(this::tryInsert)) continue;
            LOGGER.error("Couldn't load advancements: {}", advancementsToAdd);
            break;
        }
        LOGGER.info("Loaded {} advancements", (Object)this.nodes.size());
    }

    private boolean tryInsert(AdvancementHolder holder) {
        Optional<Identifier> parentId = holder.value().parent();
        AdvancementNode parentNode = parentId.map(this.nodes::get).orElse(null);
        if (parentNode == null && parentId.isPresent()) {
            return false;
        }
        AdvancementNode node = new AdvancementNode(holder, parentNode);
        if (parentNode != null) {
            parentNode.addChild(node);
        }
        this.nodes.put(holder.id(), node);
        if (parentNode == null) {
            this.roots.add(node);
            if (this.listener != null) {
                this.listener.onAddAdvancementRoot(node);
            }
        } else {
            this.tasks.add(node);
            if (this.listener != null) {
                this.listener.onAddAdvancementTask(node);
            }
        }
        return true;
    }

    public void clear() {
        this.nodes.clear();
        this.roots.clear();
        this.tasks.clear();
        if (this.listener != null) {
            this.listener.onAdvancementsCleared();
        }
    }

    public Iterable<AdvancementNode> roots() {
        return this.roots;
    }

    public Collection<AdvancementNode> nodes() {
        return this.nodes.values();
    }

    public @Nullable AdvancementNode get(Identifier id) {
        return this.nodes.get(id);
    }

    public @Nullable AdvancementNode get(AdvancementHolder advancement) {
        return this.nodes.get(advancement.id());
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        if (listener != null) {
            for (AdvancementNode root : this.roots) {
                listener.onAddAdvancementRoot(root);
            }
            for (AdvancementNode task : this.tasks) {
                listener.onAddAdvancementTask(task);
            }
        }
    }

    public static interface Listener {
        public void onAddAdvancementRoot(AdvancementNode var1);

        public void onRemoveAdvancementRoot(AdvancementNode var1);

        public void onAddAdvancementTask(AdvancementNode var1);

        public void onRemoveAdvancementTask(AdvancementNode var1);

        public void onAdvancementsCleared();
    }
}

