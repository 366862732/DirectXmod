/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.operations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.util.filefix.FileFixUtil;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.worldupdate.UpgradeProgress;

public record RegexMove(Pattern fromPattern, String toReplacement) implements FileFixOperation
{
    public RegexMove(String fromPattern, String toPattern) {
        this(Pattern.compile(fromPattern), toPattern);
    }

    @Override
    public void fix(Path baseDirectory, UpgradeProgress upgradeProgress) throws IOException {
        if (!Files.exists(baseDirectory, new LinkOption[0]) || !Files.isDirectory(baseDirectory, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> files = Files.list(baseDirectory);){
            for (Path file : files.toList()) {
                String fileName = file.getFileName().toString();
                Matcher matcher = this.fromPattern.matcher(fileName);
                if (!matcher.matches()) continue;
                String newName = matcher.replaceAll(this.toReplacement);
                FileFixUtil.moveFile(baseDirectory, fileName, newName);
            }
        }
    }
}

