/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import org.jspecify.annotations.Nullable;

public class AdvancementNode {
    private final AdvancementHolder holder;
    private final @Nullable AdvancementNode parent;
    private final Set<AdvancementNode> children = new ReferenceOpenHashSet();

    @VisibleForTesting
    public AdvancementNode(AdvancementHolder holder, @Nullable AdvancementNode parent) {
        this.holder = holder;
        this.parent = parent;
    }

    public Advancement advancement() {
        return this.holder.value();
    }

    public AdvancementHolder holder() {
        return this.holder;
    }

    public @Nullable AdvancementNode parent() {
        return this.parent;
    }

    public AdvancementNode root() {
        return AdvancementNode.getRoot(this);
    }

    public static AdvancementNode getRoot(AdvancementNode advancement) {
        AdvancementNode root = advancement;
        AdvancementNode parent;
        while ((parent = root.parent()) != null) {
            root = parent;
        }
        return root;
    }

    public Iterable<AdvancementNode> children() {
        return this.children;
    }

    @VisibleForTesting
    public void addChild(AdvancementNode child) {
        this.children.add(child);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AdvancementNode)) return false;
        AdvancementNode that = (AdvancementNode)obj;
        if (!this.holder.equals(that.holder)) return false;
        return true;
    }

    public int hashCode() {
        return this.holder.hashCode();
    }

    public String toString() {
        return this.holder.id().toString();
    }
}

