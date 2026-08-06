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

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface SearchTree<T> {
    public static <T> SearchTree<T> empty() {
        return text -> List.of();
    }

    public static <T> SearchTree<T> plainText(List<T> elements, Function<T, Stream<String>> idGetter) {
        if (elements.isEmpty()) {
            return SearchTree.empty();
        }
        SuffixArray tree = new SuffixArray();
        for (Object element : elements) {
            idGetter.apply(element).forEach(elementId -> tree.add(element, elementId.toLowerCase(Locale.ROOT)));
        }
        tree.generate();
        return tree::search;
    }

    public List<T> search(String var1);
}

