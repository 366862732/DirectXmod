/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.filefix.operations;

import java.util.List;
import java.util.Map;
import net.minecraft.util.filefix.access.FileRelation;
import net.minecraft.util.filefix.operations.ApplyInFolders;
import net.minecraft.util.filefix.operations.DeleteFileOrEmptyDirectory;
import net.minecraft.util.filefix.operations.FileFixOperation;
import net.minecraft.util.filefix.operations.GroupMove;
import net.minecraft.util.filefix.operations.Move;
import net.minecraft.util.filefix.operations.RegexMove;

public class FileFixOperations {
    public static Move moveSimple(String file) {
        return new Move(file, file);
    }

    public static Move move(String from, String to) {
        return new Move(from, to);
    }

    public static RegexMove moveRegex(String filePattern, String replacePattern) {
        return new RegexMove(filePattern, replacePattern);
    }

    public static DeleteFileOrEmptyDirectory delete(String target) {
        return new DeleteFileOrEmptyDirectory(target);
    }

    public static ApplyInFolders applyInFolders(FileRelation applicableFolders, List<FileFixOperation> operations) {
        return new ApplyInFolders(applicableFolders, operations);
    }

    public static GroupMove groupMove(Map<String, String> data, List<Move> move) {
        return new GroupMove(data, move);
    }
}

