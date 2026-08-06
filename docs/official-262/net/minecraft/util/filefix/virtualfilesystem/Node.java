/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix.virtualfilesystem;

import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFSPath;
import net.minecraft.util.filefix.virtualfilesystem.DirectoryNode;
import net.minecraft.util.filefix.virtualfilesystem.FileNode;
import org.jspecify.annotations.Nullable;

abstract sealed class Node
permits FileNode, DirectoryNode {
    protected @Nullable DirectoryNode parent;
    protected CopyOnWriteFSPath path;

    protected Node(CopyOnWriteFSPath cowPath) {
        this.setPath(cowPath);
    }

    public @Nullable String name() {
        CopyOnWriteFSPath fileName = this.path.getFileName();
        return fileName == null ? null : fileName.toString();
    }

    protected void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    protected void setPath(CopyOnWriteFSPath path) {
        this.path = path.normalize().toAbsolutePath();
    }

    public CopyOnWriteFSPath path() {
        return this.path;
    }
}

