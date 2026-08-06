/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.chunk;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.ListIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SectionTaskDynamicQueue {
    private static final int MAX_RECOMPILE_QUOTA = 2;
    private int recompileQuota = 2;
    private final List<SectionRenderDispatcher.RenderSection.SectionTask> tasks = new ObjectArrayList();

    public synchronized void add(SectionRenderDispatcher.RenderSection.SectionTask task) {
        this.tasks.add(task);
    }

    public synchronized @Nullable SectionRenderDispatcher.RenderSection.SectionTask poll(Vec3 cameraPos) {
        boolean hasInitialCompileTask;
        int bestInitialCompileTaskIndex = -1;
        int bestRecompileTaskIndex = -1;
        double bestInitialCompileDistance = Double.MAX_VALUE;
        double bestRecompileDistance = Double.MAX_VALUE;
        ListIterator<SectionRenderDispatcher.RenderSection.SectionTask> iterator = this.tasks.listIterator();
        while (iterator.hasNext()) {
            int taskIndex = iterator.nextIndex();
            SectionRenderDispatcher.RenderSection.SectionTask task = iterator.next();
            if (task.isCancelled.get()) {
                iterator.remove();
                continue;
            }
            double distance = task.getRenderOrigin().distToCenterSqr(cameraPos);
            if (!task.isRecompile() && distance < bestInitialCompileDistance) {
                bestInitialCompileDistance = distance;
                bestInitialCompileTaskIndex = taskIndex;
            }
            if (!task.isRecompile() || !(distance < bestRecompileDistance)) continue;
            bestRecompileDistance = distance;
            bestRecompileTaskIndex = taskIndex;
        }
        boolean hasRecompileTask = bestRecompileTaskIndex >= 0;
        boolean bl = hasInitialCompileTask = bestInitialCompileTaskIndex >= 0;
        if (hasRecompileTask && (!hasInitialCompileTask || this.recompileQuota > 0 && bestRecompileDistance < bestInitialCompileDistance)) {
            --this.recompileQuota;
            return this.removeTaskByIndex(bestRecompileTaskIndex);
        }
        this.recompileQuota = 2;
        return this.removeTaskByIndex(bestInitialCompileTaskIndex);
    }

    public int size() {
        return this.tasks.size();
    }

    private @Nullable SectionRenderDispatcher.RenderSection.SectionTask removeTaskByIndex(int taskIndex) {
        if (taskIndex >= 0) {
            return this.tasks.remove(taskIndex);
        }
        return null;
    }

    public synchronized void clear() {
        for (SectionRenderDispatcher.RenderSection.SectionTask task : this.tasks) {
            task.cancel();
        }
        this.tasks.clear();
    }
}

