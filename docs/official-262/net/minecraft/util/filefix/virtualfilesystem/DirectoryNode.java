/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.filefix.virtualfilesystem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFSPath;
import net.minecraft.util.filefix.virtualfilesystem.FileNode;
import net.minecraft.util.filefix.virtualfilesystem.Node;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNoSuchFileException;
import net.minecraft.util.filefix.virtualfilesystem.exception.CowFSNotDirectoryException;
import org.jspecify.annotations.Nullable;

final class DirectoryNode
extends Node {
    private final Map<String, Node> childNodes = new HashMap<String, Node>();

    public DirectoryNode(CopyOnWriteFSPath path) {
        super(path);
    }

    public Collection<Node> children() {
        return Collections.unmodifiableCollection(this.childNodes.values());
    }

    public void addChild(Node child) {
        String name = Objects.requireNonNull(child.name());
        this.childNodes.put(name, child);
        child.setParent(this);
    }

    void removeChild(String name) {
        this.childNodes.remove(name);
    }

    public @Nullable Node getChild(String name) {
        return this.childNodes.get(name);
    }

    public DirectoryNode directoryByPath(CopyOnWriteFSPath path) throws CowFSNoSuchFileException, CowFSNotDirectoryException {
        Node node = this.byPath(path);
        if (node instanceof DirectoryNode) {
            DirectoryNode result = (DirectoryNode)node;
            return result;
        }
        throw new CowFSNotDirectoryException(String.valueOf(path) + " was a file, expected directory");
    }

    public FileNode fileByPath(CopyOnWriteFSPath path) throws CowFSNoSuchFileException {
        Node node = this.byPathOrNull(path);
        if (node instanceof FileNode) {
            FileNode result = (FileNode)node;
            return result;
        }
        throw new CowFSNoSuchFileException(path.toString());
    }

    public Node byPath(CopyOnWriteFSPath path) throws CowFSNoSuchFileException {
        Node node = this.byPathOrNull(path);
        if (node != null) {
            return node;
        }
        throw new CowFSNoSuchFileException(path.toString());
    }

    public @Nullable Node byPathOrNull(CopyOnWriteFSPath path) {
        int nameCount = path.getNameCount();
        DirectoryNode directory = this;
        for (int i = 0; i < nameCount; ++i) {
            String name = path.getName(i).toString();
            if (name.equals(".")) continue;
            if (name.equals("..")) {
                DirectoryNode parent = directory.parent;
                if (parent == null) continue;
                directory = parent;
                continue;
            }
            Node nextNode = directory.getChild(name);
            if (nextNode instanceof DirectoryNode) {
                DirectoryNode nextDirectory;
                directory = nextDirectory = (DirectoryNode)nextNode;
                continue;
            }
            return i == nameCount - 1 ? nextNode : null;
        }
        return directory;
    }
}

