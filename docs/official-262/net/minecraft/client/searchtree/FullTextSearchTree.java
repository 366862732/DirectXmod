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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.searchtree.IdSearchTree;
import net.minecraft.client.searchtree.IntersectionIterator;
import net.minecraft.client.searchtree.MergingUniqueIterator;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public class FullTextSearchTree<T>
extends IdSearchTree<T> {
    private final SearchTree<T> plainTextSearchTree;

    public FullTextSearchTree(Function<T, Stream<String>> nameGetter, Function<T, Stream<Identifier>> idGetter, List<T> contents) {
        super(idGetter, contents);
        this.plainTextSearchTree = SearchTree.plainText(contents, nameGetter);
    }

    @Override
    protected List<T> searchPlainText(String text) {
        return this.plainTextSearchTree.search(text);
    }

    @Override
    protected List<T> searchIdentifier(String namespace, String path) {
        List namespaces = this.identifierSearchTree.searchNamespace(namespace);
        List paths = this.identifierSearchTree.searchPath(path);
        List<T> names = this.plainTextSearchTree.search(path);
        MergingUniqueIterator mergedPathsAndNames = new MergingUniqueIterator(paths.iterator(), names.iterator(), this.additionOrder);
        return ImmutableList.copyOf(new IntersectionIterator(namespaces.iterator(), mergedPathsAndNames, this.additionOrder));
    }
}

