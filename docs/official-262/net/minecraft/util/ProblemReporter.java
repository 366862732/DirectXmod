/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.HashMultimap
 *  com.google.common.collect.Multimap
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface ProblemReporter {
    public static final ProblemReporter DISCARDING = new ProblemReporter(){

        @Override
        public ProblemReporter forChild(PathElement path) {
            return this;
        }

        @Override
        public void report(Problem problem) {
        }
    };

    public ProblemReporter forChild(PathElement var1);

    public void report(Problem var1);

    public static class ScopedCollector
    extends Collector
    implements AutoCloseable {
        private final Logger logger;

        public ScopedCollector(Logger logger) {
            this.logger = logger;
        }

        public ScopedCollector(PathElement root, Logger logger) {
            super(root);
            this.logger = logger;
        }

        @Override
        public void close() {
            if (!this.isEmpty()) {
                this.logger.warn("[{}] Serialization errors:\n{}", (Object)this.logger.getName(), (Object)this.getTreeReport());
            }
        }
    }

    public static class Collector
    implements ProblemReporter {
        public static final PathElement EMPTY_ROOT = () -> "";
        private final @Nullable Collector parent;
        private final PathElement element;
        private final Set<Entry> problems;

        public Collector() {
            this(EMPTY_ROOT);
        }

        public Collector(PathElement root) {
            this.parent = null;
            this.problems = new LinkedHashSet<Entry>();
            this.element = root;
        }

        private Collector(Collector parent, PathElement path) {
            this.problems = parent.problems;
            this.parent = parent;
            this.element = path;
        }

        @Override
        public ProblemReporter forChild(PathElement path) {
            return new Collector(this, path);
        }

        @Override
        public void report(Problem problem) {
            this.problems.add(new Entry(this, problem));
        }

        public boolean isEmpty() {
            return this.problems.isEmpty();
        }

        public void forEach(BiConsumer<String, Problem> output) {
            ArrayList<PathElement> pathElements = new ArrayList<PathElement>();
            StringBuilder pathString = new StringBuilder();
            for (Entry entry : this.problems) {
                Collector current = entry.source;
                while (current != null) {
                    pathElements.add(current.element);
                    current = current.parent;
                }
                for (int i = pathElements.size() - 1; i >= 0; --i) {
                    pathString.append(((PathElement)pathElements.get(i)).get());
                }
                output.accept(pathString.toString(), entry.problem());
                pathString.setLength(0);
                pathElements.clear();
            }
        }

        public String getReport() {
            HashMultimap groupedProblems = HashMultimap.create();
            this.forEach((arg_0, arg_1) -> ((Multimap)groupedProblems).put(arg_0, arg_1));
            return groupedProblems.asMap().entrySet().stream().map(entry -> " at " + (String)entry.getKey() + ": " + ((Collection)entry.getValue()).stream().map(Problem::description).collect(Collectors.joining("; "))).collect(Collectors.joining("\n"));
        }

        public String getTreeReport() {
            ArrayList<PathElement> pathElements = new ArrayList<PathElement>();
            ProblemTreeNode root = new ProblemTreeNode(this.element);
            for (Entry entry : this.problems) {
                Collector current = entry.source;
                while (current != this) {
                    pathElements.add(current.element);
                    current = current.parent;
                }
                ProblemTreeNode node = root;
                for (int i = pathElements.size() - 1; i >= 0; --i) {
                    node = node.child((PathElement)pathElements.get(i));
                }
                pathElements.clear();
                node.problems.add(entry.problem);
            }
            return String.join((CharSequence)"\n", root.getLines());
        }

        private record Entry(Collector source, Problem problem) {
        }

        private record ProblemTreeNode(PathElement element, List<Problem> problems, Map<PathElement, ProblemTreeNode> children) {
            public ProblemTreeNode(PathElement pathElement) {
                this(pathElement, new ArrayList<Problem>(), new LinkedHashMap<PathElement, ProblemTreeNode>());
            }

            public ProblemTreeNode child(PathElement id) {
                return this.children.computeIfAbsent(id, ProblemTreeNode::new);
            }

            public List<String> getLines() {
                int problemCount = this.problems.size();
                int childrenCount = this.children.size();
                if (problemCount == 0 && childrenCount == 0) {
                    return List.of();
                }
                if (problemCount == 0 && childrenCount == 1) {
                    ArrayList<String> lines = new ArrayList<String>();
                    this.children.forEach((element, child) -> lines.addAll(child.getLines()));
                    lines.set(0, this.element.get() + (String)lines.get(0));
                    return lines;
                }
                if (problemCount == 1 && childrenCount == 0) {
                    return List.of(this.element.get() + ": " + this.problems.getFirst().description());
                }
                ArrayList<String> lines = new ArrayList<String>();
                this.children.forEach((element, child) -> lines.addAll(child.getLines()));
                lines.replaceAll(s -> "  " + s);
                for (Problem problem : this.problems) {
                    lines.add("  " + problem.description());
                }
                lines.addFirst(this.element.get() + ":");
                return lines;
            }
        }
    }

    public record ElementReferencePathElement(ResourceKey<?> id) implements PathElement
    {
        @Override
        public String get() {
            return "->{" + String.valueOf(this.id.identifier()) + "@" + String.valueOf(this.id.registry()) + "}";
        }
    }

    public record MapEntryPathElement(String name, String key) implements PathElement
    {
        @Override
        public String get() {
            return "." + this.name + "[" + this.key + "]";
        }
    }

    public record IndexedPathElement(int index) implements PathElement
    {
        @Override
        public String get() {
            return "[" + this.index + "]";
        }
    }

    public record IndexedFieldPathElement(String name, int index) implements PathElement
    {
        @Override
        public String get() {
            return "." + this.name + "[" + this.index + "]";
        }
    }

    public record FieldPathElement(String name) implements PathElement
    {
        @Override
        public String get() {
            return "." + this.name;
        }
    }

    public record RootElementPathElement(ResourceKey<?> id) implements PathElement
    {
        @Override
        public String get() {
            return "{" + String.valueOf(this.id.identifier()) + "@" + String.valueOf(this.id.registry()) + "}";
        }
    }

    public record RootFieldPathElement(String name) implements PathElement
    {
        @Override
        public String get() {
            return this.name;
        }
    }

    @FunctionalInterface
    public static interface PathElement {
        public String get();
    }

    public static interface Problem {
        public String description();
    }
}

