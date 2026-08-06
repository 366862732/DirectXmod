/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.filefix.operations.Move;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record GroupMove(Map<String, String> fromTo, List<Move> fixers) implements FileFixOperation
{
    @Override
    public void fix(Path baseDirectory, UpgradeProgress upgradeProgress) throws IOException {
        for (Map.Entry<String, String> entry : this.fromTo.entrySet()) {
            for (Move moveOperation : this.fixers) {
                Move relative = moveOperation.relative(entry.getKey(), entry.getValue());
                relative.fix(baseDirectory, upgradeProgress);
            }
        }
    }
}

