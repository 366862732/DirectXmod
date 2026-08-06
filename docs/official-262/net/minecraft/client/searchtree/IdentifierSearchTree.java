/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.searchtree;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.searchtree.SuffixArray;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public interface IdentifierSearchTree<T> {
    public static <T> IdentifierSearchTree<T> empty() {
        return new IdentifierSearchTree<T>(){

            @Override
            public List<T> searchNamespace(String namespace) {
                return List.of();
            }

            @Override
            public List<T> searchPath(String path) {
                return List.of();
            }
        };
    }

    public static <T> IdentifierSearchTree<T> create(List<T> elements, Function<T, Stream<Identifier>> idGetter) {
        if (elements.isEmpty()) {
            return IdentifierSearchTree.empty();
        }
        final SuffixArray namespaceTree = new SuffixArray();
        final SuffixArray pathTree = new SuffixArray();
        for (Object element : elements) {
            idGetter.apply(element).forEach(elementId -> {
                namespaceTree.add(element, elementId.getNamespace().toLowerCase(Locale.ROOT));
                pathTree.add(element, elementId.getPath().toLowerCase(Locale.ROOT));
            });
        }
        namespaceTree.generate();
        pathTree.generate();
        return new IdentifierSearchTree<T>(){

            @Override
            public List<T> searchNamespace(String namespace) {
                return namespaceTree.search(namespace);
            }

            @Override
            public List<T> searchPath(String path) {
                return pathTree.search(path);
            }
        };
    }

    public List<T> searchNamespace(String var1);

    public List<T> searchPath(String var1);
}

