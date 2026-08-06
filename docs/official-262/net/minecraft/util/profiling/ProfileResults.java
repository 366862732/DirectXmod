/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.profiling;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.util.profiling.ResultField;

public interface ProfileResults {
    public static final char PATH_SEPARATOR = '\u001e';

    public List<ResultField> getTimes(String var1);

    public boolean saveResults(Path var1);

    public long getStartTimeNano();

    public int getStartTimeTicks();

    public long getEndTimeNano();

    public int getEndTimeTicks();

    default public long getNanoDuration() {
        return this.getEndTimeNano() - this.getStartTimeNano();
    }

    default public int getTickDuration() {
        return this.getEndTimeTicks() - this.getStartTimeTicks();
    }

    public String getProfilerResults();

    public static String demanglePath(String path) {
        return path.replace('\u001e', '.');
    }
}

