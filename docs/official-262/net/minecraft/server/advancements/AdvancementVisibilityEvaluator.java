/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.Stack
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 */
package net.minecraft.server.advancements;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;

public class AdvancementVisibilityEvaluator {
    private static final int VISIBILITY_DEPTH = 2;

    private static VisibilityRule evaluateVisibilityRule(Advancement advancement, boolean isDone) {
        Optional<DisplayInfo> display = advancement.display();
        if (display.isEmpty()) {
            return VisibilityRule.HIDE;
        }
        if (isDone) {
            return VisibilityRule.SHOW;
        }
        if (display.get().isHidden()) {
            return VisibilityRule.HIDE;
        }
        return VisibilityRule.NO_CHANGE;
    }

    private static boolean evaluateVisiblityForUnfinishedNode(Stack<VisibilityRule> ascendants) {
        for (int i = 0; i <= 2; ++i) {
            VisibilityRule visibility = (VisibilityRule)((Object)ascendants.peek(i));
            if (visibility == VisibilityRule.SHOW) {
                return true;
            }
            if (visibility != VisibilityRule.HIDE) continue;
            return false;
        }
        return false;
    }

    private static boolean evaluateVisibility(AdvancementNode node, Stack<VisibilityRule> ascendants, Predicate<AdvancementNode> isDoneTest, Output output) {
        boolean isSelfDone = isDoneTest.test(node);
        VisibilityRule descendantVisibility = AdvancementVisibilityEvaluator.evaluateVisibilityRule(node.advancement(), isSelfDone);
        boolean isSelfOrDescendantDone = isSelfDone;
        ascendants.push((Object)descendantVisibility);
        for (AdvancementNode child : node.children()) {
            isSelfOrDescendantDone |= AdvancementVisibilityEvaluator.evaluateVisibility(child, ascendants, isDoneTest, output);
        }
        boolean visiblity = isSelfOrDescendantDone || AdvancementVisibilityEvaluator.evaluateVisiblityForUnfinishedNode(ascendants);
        ascendants.pop();
        output.accept(node, visiblity);
        return isSelfOrDescendantDone;
    }

    public static void evaluateVisibility(AdvancementNode node, Predicate<AdvancementNode> isDone, Output output) {
        AdvancementNode root = node.root();
        ObjectArrayList visibilityStack = new ObjectArrayList();
        for (int i = 0; i <= 2; ++i) {
            visibilityStack.push((Object)VisibilityRule.NO_CHANGE);
        }
        AdvancementVisibilityEvaluator.evaluateVisibility(root, (Stack<VisibilityRule>)visibilityStack, isDone, output);
    }

    private static enum VisibilityRule {
        SHOW,
        HIDE,
        NO_CHANGE;

    }

    @FunctionalInterface
    public static interface Output {
        public void accept(AdvancementNode var1, boolean var2);
    }
}

