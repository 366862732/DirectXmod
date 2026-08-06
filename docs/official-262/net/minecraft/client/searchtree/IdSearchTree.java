/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.searchtree.IdentifierSearchTree;
import net.minecraft.client.searchtree.IntersectionIterator;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class IdSearchTree<T>
implements SearchTree<T> {
    protected final Comparator<T> additionOrder;
    protected final IdentifierSearchTree<T> identifierSearchTree;

    public IdSearchTree(Function<T, Stream<Identifier>> idGetter, List<T> contents) {
        ToIntFunction<T> indexLookup = Util.createIndexLookup(contents);
        this.additionOrder = Comparator.comparingInt(indexLookup);
        this.identifierSearchTree = IdentifierSearchTree.create(contents, idGetter);
    }

    @Override
    public List<T> search(String text) {
        int colon = text.indexOf(58);
        if (colon == -1) {
            return this.searchPlainText(text);
        }
        return this.searchIdentifier(text.substring(0, colon).trim(), text.substring(colon + 1).trim());
    }

    protected List<T> searchPlainText(String text) {
        return this.identifierSearchTree.searchPath(text);
    }

    protected List<T> searchIdentifier(String namespace, String path) {
        List<T> namespaces = this.identifierSearchTree.searchNamespace(namespace);
        List<T> paths = this.identifierSearchTree.searchPath(path);
        return ImmutableList.copyOf(new IntersectionIterator<T>(namespaces.iterator(), paths.iterator(), this.additionOrder));
    }
}

