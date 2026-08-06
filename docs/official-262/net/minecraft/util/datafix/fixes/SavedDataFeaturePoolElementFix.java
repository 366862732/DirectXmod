/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.OptionalDynamic
 */
package net.minecraft.util.datafix.fixes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.util.datafix.fixes.References;

public class SavedDataFeaturePoolElementFix
extends DataFix {
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)\\]");
    private static final Set<String> PIECE_TYPE = Sets.newHashSet((Object[])new String[]{"minecraft:jigsaw", "minecraft:nvi", "minecraft:pcp", "minecraft:bastionremnant", "minecraft:runtime"});
    private static final Set<String> FEATURES = Sets.newHashSet((Object[])new String[]{"minecraft:tree", "minecraft:flower", "minecraft:block_pile", "minecraft:random_patch"});

    public SavedDataFeaturePoolElementFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    public TypeRewriteRule makeRule() {
        return this.writeFixAndRead("SavedDataFeaturePoolElementFix", this.getInputSchema().getType(References.STRUCTURE_FEATURE), this.getOutputSchema().getType(References.STRUCTURE_FEATURE), SavedDataFeaturePoolElementFix::fixTag);
    }

    private static <T> Dynamic<T> fixTag(Dynamic<T> input) {
        return input.update("Children", SavedDataFeaturePoolElementFix::updateChildren);
    }

    private static <T> Dynamic<T> updateChildren(Dynamic<T> input) {
        return input.asStreamOpt().map(SavedDataFeaturePoolElementFix::updateChildren).map(arg_0 -> input.createList(arg_0)).result().orElse(input);
    }

    private static Stream<? extends Dynamic<?>> updateChildren(Stream<? extends Dynamic<?>> stream) {
        return stream.map(child -> {
            String id = child.get("id").asString("");
            if (!PIECE_TYPE.contains(id)) {
                return child;
            }
            OptionalDynamic poolElement = child.get("pool_element");
            if (!poolElement.get("element_type").asString("").equals("minecraft:feature_pool_element")) {
                return child;
            }
            return child.update("pool_element", pool -> pool.update("feature", SavedDataFeaturePoolElementFix::fixFeature));
        });
    }

    private static <T> OptionalDynamic<T> get(Dynamic<T> input, String ... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Missing path");
        }
        OptionalDynamic output = input.get(path[0]);
        for (int i = 1; i < path.length; ++i) {
            String element = path[i];
            Matcher matcher = INDEX_PATTERN.matcher(element);
            if (matcher.matches()) {
                int id = Integer.parseInt(matcher.group(1));
                List dynamics = output.asList(Function.identity());
                if (id >= 0 && id < dynamics.size()) {
                    output = new OptionalDynamic(input.getOps(), DataResult.success((Object)((Dynamic)dynamics.get(id))));
                    continue;
                }
                output = new OptionalDynamic(input.getOps(), DataResult.error(() -> "Missing id:" + id));
                continue;
            }
            output = output.get(element);
        }
        return output;
    }

    @VisibleForTesting
    protected static Dynamic<?> fixFeature(Dynamic<?> value) {
        Optional<String> replacement = SavedDataFeaturePoolElementFix.getReplacement(SavedDataFeaturePoolElementFix.get(value, "type").asString(""), SavedDataFeaturePoolElementFix.get(value, "name").asString(""), SavedDataFeaturePoolElementFix.get(value, "config", "state_provider", "type").asString(""), SavedDataFeaturePoolElementFix.get(value, "config", "state_provider", "state", "Name").asString(""), SavedDataFeaturePoolElementFix.get(value, "config", "state_provider", "entries", "[0]", "data", "Name").asString(""), SavedDataFeaturePoolElementFix.get(value, "config", "foliage_placer", "type").asString(""), SavedDataFeaturePoolElementFix.get(value, "config", "leaves_provider", "state", "Name").asString(""));
        if (replacement.isPresent()) {
            return value.createString(replacement.get());
        }
        return value;
    }

    private static Optional<String> getReplacement(String type, String name, String stateProviderType, String stateProviderState, String stateProviderFirstWeighedState, String foliagePlacerType, String leavesProviderState) {
        String feature;
        if (!type.isEmpty()) {
            feature = type;
        } else if (!name.isEmpty()) {
            feature = "minecraft:normal_tree".equals(name) ? "minecraft:tree" : name;
        } else {
            return Optional.empty();
        }
        if (FEATURES.contains(feature)) {
            if ("minecraft:random_patch".equals(feature)) {
                if ("minecraft:simple_state_provider".equals(stateProviderType)) {
                    if ("minecraft:sweet_berry_bush".equals(stateProviderState)) {
                        return Optional.of("minecraft:patch_berry_bush");
                    }
                    if ("minecraft:cactus".equals(stateProviderState)) {
                        return Optional.of("minecraft:patch_cactus");
                    }
                } else if ("minecraft:weighted_state_provider".equals(stateProviderType) && ("minecraft:grass".equals(stateProviderFirstWeighedState) || "minecraft:fern".equals(stateProviderFirstWeighedState))) {
                    return Optional.of("minecraft:patch_taiga_grass");
                }
            } else if ("minecraft:block_pile".equals(feature)) {
                if ("minecraft:simple_state_provider".equals(stateProviderType) || "minecraft:rotated_block_provider".equals(stateProviderType)) {
                    if ("minecraft:hay_block".equals(stateProviderState)) {
                        return Optional.of("minecraft:pile_hay");
                    }
                    if ("minecraft:melon".equals(stateProviderState)) {
                        return Optional.of("minecraft:pile_melon");
                    }
                    if ("minecraft:snow".equals(stateProviderState)) {
                        return Optional.of("minecraft:pile_snow");
                    }
                } else if ("minecraft:weighted_state_provider".equals(stateProviderType)) {
                    if ("minecraft:packed_ice".equals(stateProviderFirstWeighedState) || "minecraft:blue_ice".equals(stateProviderFirstWeighedState)) {
                        return Optional.of("minecraft:pile_ice");
                    }
                    if ("minecraft:jack_o_lantern".equals(stateProviderFirstWeighedState) || "minecraft:pumpkin".equals(stateProviderFirstWeighedState)) {
                        return Optional.of("minecraft:pile_pumpkin");
                    }
                }
            } else {
                if ("minecraft:flower".equals(feature)) {
                    return Optional.of("minecraft:flower_plain");
                }
                if ("minecraft:tree".equals(feature)) {
                    if ("minecraft:acacia_foliage_placer".equals(foliagePlacerType)) {
                        return Optional.of("minecraft:acacia");
                    }
                    if ("minecraft:blob_foliage_placer".equals(foliagePlacerType) && "minecraft:oak_leaves".equals(leavesProviderState)) {
                        return Optional.of("minecraft:oak");
                    }
                    if ("minecraft:pine_foliage_placer".equals(foliagePlacerType)) {
                        return Optional.of("minecraft:pine");
                    }
                    if ("minecraft:spruce_foliage_placer".equals(foliagePlacerType)) {
                        return Optional.of("minecraft:spruce");
                    }
                }
            }
        }
        return Optional.empty();
    }
}

