/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
 */
package net.minecraft.world.entity.ai.goal;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

public class GoalSelector {
    private static final WrappedGoal NO_GOAL = new WrappedGoal(Integer.MAX_VALUE, new Goal(){

        @Override
        public boolean canUse() {
            return false;
        }
    }){

        @Override
        public boolean isRunning() {
            return false;
        }
    };
    private final Map<Goal.Flag, WrappedGoal> lockedFlags = new EnumMap<Goal.Flag, WrappedGoal>(Goal.Flag.class);
    private final Set<WrappedGoal> availableGoals = new ObjectLinkedOpenHashSet();
    private final EnumSet<Goal.Flag> disabledFlags = EnumSet.noneOf(Goal.Flag.class);

    public void addGoal(int prio, Goal goal) {
        this.availableGoals.add(new WrappedGoal(prio, goal));
    }

    public void removeAllGoals(Predicate<Goal> predicate) {
        for (WrappedGoal availableGoal : this.availableGoals) {
            if (!predicate.test(availableGoal.getGoal()) || !availableGoal.isRunning()) continue;
            availableGoal.stop();
        }
        this.availableGoals.removeIf(goal -> predicate.test(goal.getGoal()));
    }

    public void removeGoal(Goal toRemove) {
        this.removeAllGoals(goal -> goal == toRemove);
    }

    private static boolean goalContainsAnyFlags(WrappedGoal goal, EnumSet<Goal.Flag> disabledFlags) {
        for (Goal.Flag flag : goal.getFlags()) {
            if (!disabledFlags.contains((Object)flag)) continue;
            return true;
        }
        return false;
    }

    private static boolean goalCanBeReplacedForAllFlags(WrappedGoal goal, Map<Goal.Flag, WrappedGoal> lockedFlags) {
        for (Goal.Flag flag : goal.getFlags()) {
            if (lockedFlags.getOrDefault((Object)flag, NO_GOAL).canBeReplacedBy(goal)) continue;
            return false;
        }
        return true;
    }

    public void tick() {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("goalCleanup");
        for (WrappedGoal goal : this.availableGoals) {
            if (!goal.isRunning() || !GoalSelector.goalContainsAnyFlags(goal, this.disabledFlags) && goal.canContinueToUse()) continue;
            goal.stop();
        }
        this.lockedFlags.entrySet().removeIf(entry -> !((WrappedGoal)entry.getValue()).isRunning());
        profiler.pop();
        profiler.push("goalUpdate");
        for (WrappedGoal goal : this.availableGoals) {
            if (goal.isRunning() || GoalSelector.goalContainsAnyFlags(goal, this.disabledFlags) || !GoalSelector.goalCanBeReplacedForAllFlags(goal, this.lockedFlags) || !goal.canUse()) continue;
            for (Goal.Flag flag : goal.getFlags()) {
                WrappedGoal currentGoal = this.lockedFlags.getOrDefault((Object)flag, NO_GOAL);
                currentGoal.stop();
                this.lockedFlags.put(flag, goal);
            }
            goal.start();
        }
        profiler.pop();
        this.tickRunningGoals(true);
    }

    public void tickRunningGoals(boolean forceTickAllRunningGoals) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("goalTick");
        for (WrappedGoal goal : this.availableGoals) {
            if (!goal.isRunning() || !forceTickAllRunningGoals && !goal.requiresUpdateEveryTick()) continue;
            goal.tick();
        }
        profiler.pop();
    }

    public Set<WrappedGoal> getAvailableGoals() {
        return this.availableGoals;
    }

    public void disableControlFlag(Goal.Flag flag) {
        this.disabledFlags.add(flag);
    }

    public void enableControlFlag(Goal.Flag flag) {
        this.disabledFlags.remove((Object)flag);
    }

    public void setControlFlag(Goal.Flag flag, boolean enabled) {
        if (enabled) {
            this.enableControlFlag(flag);
        } else {
            this.disableControlFlag(flag);
        }
    }
}

