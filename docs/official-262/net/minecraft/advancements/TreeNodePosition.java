/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.advancements.AdvancementNode;
import org.jspecify.annotations.Nullable;

public class TreeNodePosition {
    private final AdvancementNode node;
    private final @Nullable TreeNodePosition parent;
    private final @Nullable TreeNodePosition previousSibling;
    private final int childIndex;
    private final List<TreeNodePosition> children = Lists.newArrayList();
    private TreeNodePosition ancestor;
    private @Nullable TreeNodePosition thread;
    private int x;
    private float y;
    private float mod;
    private float change;
    private float shift;

    public TreeNodePosition(AdvancementNode node, @Nullable TreeNodePosition parent, @Nullable TreeNodePosition previousSibling, int childIndex, int depth) {
        if (node.advancement().display().isEmpty()) {
            throw new IllegalArgumentException("Can't position an invisible advancement!");
        }
        this.node = node;
        this.parent = parent;
        this.previousSibling = previousSibling;
        this.childIndex = childIndex;
        this.ancestor = this;
        this.x = depth;
        this.y = -1.0f;
        TreeNodePosition previous = null;
        for (AdvancementNode child : node.children()) {
            previous = this.addChild(child, previous);
        }
    }

    private @Nullable TreeNodePosition addChild(AdvancementNode node, @Nullable TreeNodePosition previous) {
        if (node.advancement().display().isPresent()) {
            previous = new TreeNodePosition(node, this, previous, this.children.size() + 1, this.x + 1);
            this.children.add(previous);
        } else {
            for (AdvancementNode grandchild : node.children()) {
                previous = this.addChild(grandchild, previous);
            }
        }
        return previous;
    }

    private void firstWalk() {
        if (this.children.isEmpty()) {
            this.y = this.previousSibling != null ? this.previousSibling.y + 1.0f : 0.0f;
            return;
        }
        TreeNodePosition defaultAncestor = null;
        for (TreeNodePosition child : this.children) {
            child.firstWalk();
            defaultAncestor = child.apportion(defaultAncestor == null ? child : defaultAncestor);
        }
        this.executeShifts();
        float midpoint = (this.children.get((int)0).y + this.children.get((int)(this.children.size() - 1)).y) / 2.0f;
        if (this.previousSibling != null) {
            this.y = this.previousSibling.y + 1.0f;
            this.mod = this.y - midpoint;
        } else {
            this.y = midpoint;
        }
    }

    private float secondWalk(float modSum, int depth, float min) {
        this.y += modSum;
        this.x = depth;
        if (this.y < min) {
            min = this.y;
        }
        for (TreeNodePosition child : this.children) {
            min = child.secondWalk(modSum + this.mod, depth + 1, min);
        }
        return min;
    }

    private void thirdWalk(float offset) {
        this.y += offset;
        for (TreeNodePosition child : this.children) {
            child.thirdWalk(offset);
        }
    }

    private void executeShifts() {
        float shift = 0.0f;
        float change = 0.0f;
        for (int i = this.children.size() - 1; i >= 0; --i) {
            TreeNodePosition child = this.children.get(i);
            child.y += shift;
            child.mod += shift;
            shift += child.shift + (change += child.change);
        }
    }

    private @Nullable TreeNodePosition previousOrThread() {
        if (this.thread != null) {
            return this.thread;
        }
        if (!this.children.isEmpty()) {
            return this.children.get(0);
        }
        return null;
    }

    private @Nullable TreeNodePosition nextOrThread() {
        if (this.thread != null) {
            return this.thread;
        }
        if (!this.children.isEmpty()) {
            return this.children.get(this.children.size() - 1);
        }
        return null;
    }

    private TreeNodePosition apportion(TreeNodePosition defaultAncestor) {
        if (this.previousSibling == null) {
            return defaultAncestor;
        }
        TreeNodePosition vir = this;
        TreeNodePosition vor = this;
        TreeNodePosition vil = this.previousSibling;
        TreeNodePosition vol = this.parent.children.get(0);
        float sir = this.mod;
        float sor = this.mod;
        float sil = vil.mod;
        float sol = vol.mod;
        while (vil.nextOrThread() != null && vir.previousOrThread() != null) {
            vil = vil.nextOrThread();
            vir = vir.previousOrThread();
            vol = vol.previousOrThread();
            vor = vor.nextOrThread();
            vor.ancestor = this;
            float shift = vil.y + sil - (vir.y + sir) + 1.0f;
            if (shift > 0.0f) {
                vil.getAncestor(this, defaultAncestor).moveSubtree(this, shift);
                sir += shift;
                sor += shift;
            }
            sil += vil.mod;
            sir += vir.mod;
            sol += vol.mod;
            sor += vor.mod;
        }
        if (vil.nextOrThread() != null && vor.nextOrThread() == null) {
            vor.thread = vil.nextOrThread();
            vor.mod += sil - sor;
        } else {
            if (vir.previousOrThread() != null && vol.previousOrThread() == null) {
                vol.thread = vir.previousOrThread();
                vol.mod += sir - sol;
            }
            defaultAncestor = this;
        }
        return defaultAncestor;
    }

    private void moveSubtree(TreeNodePosition right, float shift) {
        float subtrees = right.childIndex - this.childIndex;
        if (subtrees != 0.0f) {
            right.change -= shift / subtrees;
            this.change += shift / subtrees;
        }
        right.shift += shift;
        right.y += shift;
        right.mod += shift;
    }

    private TreeNodePosition getAncestor(TreeNodePosition other, TreeNodePosition defaultAncestor) {
        if (this.ancestor != null && other.parent.children.contains(this.ancestor)) {
            return this.ancestor;
        }
        return defaultAncestor;
    }

    private void finalizePosition() {
        this.node.advancement().display().ifPresent(display -> display.setLocation(this.x, this.y));
        if (!this.children.isEmpty()) {
            for (TreeNodePosition child : this.children) {
                child.finalizePosition();
            }
        }
    }

    public static void run(AdvancementNode node) {
        if (node.advancement().display().isEmpty()) {
            throw new IllegalArgumentException("Can't position children of an invisible root!");
        }
        TreeNodePosition root = new TreeNodePosition(node, null, null, 1, 0);
        root.firstWalk();
        float min = root.secondWalk(0.0f, 0, root.y);
        if (min < 0.0f) {
            root.thirdWalk(-min);
        }
        root.finalizePosition();
    }
}

