/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block.dispatch.multipart;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class MultiPartModel
implements BlockStateModel {
    private final SharedBakedState shared;
    private final BlockState blockState;
    private @Nullable List<BlockStateModel> models;

    private MultiPartModel(SharedBakedState shared, BlockState blockState) {
        this.shared = shared;
        this.blockState = blockState;
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.shared.particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.shared.materialFlags;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        if (this.models == null) {
            this.models = this.shared.selectModels(this.blockState);
        }
        long seed = random.nextLong();
        for (BlockStateModel model : this.models) {
            random.setSeed(seed);
            model.collectParts(random, output);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class SharedBakedState {
        private final List<Selector<BlockStateModel>> selectors;
        private final Material.Baked particleMaterial;
        private final @BakedQuad.MaterialFlags int materialFlags;
        private final Map<BitSet, List<BlockStateModel>> subsets = new ConcurrentHashMap<BitSet, List<BlockStateModel>>();

        private static BlockStateModel getFirstModel(List<Selector<BlockStateModel>> selectors) {
            if (selectors.isEmpty()) {
                throw new IllegalArgumentException("Model must have at least one selector");
            }
            return selectors.getFirst().model();
        }

        private static @BakedQuad.MaterialFlags int computeMaterialFlags(List<Selector<BlockStateModel>> selectors) {
            int flags = 0;
            for (Selector<BlockStateModel> selector : selectors) {
                flags |= ((BlockStateModel)selector.model).materialFlags();
            }
            return flags;
        }

        public SharedBakedState(List<Selector<BlockStateModel>> selectors) {
            this.selectors = selectors;
            BlockStateModel firstModel = SharedBakedState.getFirstModel(selectors);
            this.particleMaterial = firstModel.particleMaterial();
            this.materialFlags = SharedBakedState.computeMaterialFlags(selectors);
        }

        public List<BlockStateModel> selectModels(BlockState state) {
            BitSet selectedModels = new BitSet();
            for (int i = 0; i < this.selectors.size(); ++i) {
                if (!this.selectors.get((int)i).condition.test(state)) continue;
                selectedModels.set(i);
            }
            return this.subsets.computeIfAbsent(selectedModels, selected -> {
                ImmutableList.Builder result = ImmutableList.builder();
                for (int i = 0; i < this.selectors.size(); ++i) {
                    if (!selected.get(i)) continue;
                    result.add((Object)((BlockStateModel)this.selectors.get((int)i).model));
                }
                return result.build();
            });
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Unbaked
    implements BlockStateModel.UnbakedRoot {
        private final List<Selector<BlockStateModel.Unbaked>> selectors;
        private final ModelBaker.SharedOperationKey<SharedBakedState> sharedStateKey = new ModelBaker.SharedOperationKey<SharedBakedState>(this){
            final /* synthetic */ Unbaked this$0;
            {
                Unbaked unbaked = this$0;
                Objects.requireNonNull(unbaked);
                this.this$0 = unbaked;
            }

            @Override
            public SharedBakedState compute(ModelBaker modelBakery) {
                ImmutableList.Builder selectors = ImmutableList.builderWithExpectedSize((int)this.this$0.selectors.size());
                for (Selector<BlockStateModel.Unbaked> selector : this.this$0.selectors) {
                    selectors.add(selector.with(((BlockStateModel.Unbaked)selector.model).bake(modelBakery)));
                }
                return new SharedBakedState((List<Selector<BlockStateModel>>)selectors.build());
            }
        };

        public Unbaked(List<Selector<BlockStateModel.Unbaked>> selectors) {
            this.selectors = selectors;
        }

        @Override
        public Object visualEqualityGroup(BlockState blockState) {
            IntArrayList triggeredSelectors = new IntArrayList();
            for (int i = 0; i < this.selectors.size(); ++i) {
                if (!this.selectors.get((int)i).condition.test(blockState)) continue;
                triggeredSelectors.add(i);
            }
            @Environment(value=EnvType.CLIENT)
            record Key(Unbaked model, IntList selectors) {
            }
            return new Key(this, (IntList)triggeredSelectors);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            this.selectors.forEach(s -> ((BlockStateModel.Unbaked)s.model).resolveDependencies(resolver));
        }

        @Override
        public BlockStateModel bake(BlockState blockState, ModelBaker modelBakery) {
            SharedBakedState shared = modelBakery.compute(this.sharedStateKey);
            return new MultiPartModel(shared, blockState);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Selector<T>(Predicate<BlockState> condition, T model) {
        public <S> Selector<S> with(S newModel) {
            return new Selector<S>(this.condition, newModel);
        }
    }
}

