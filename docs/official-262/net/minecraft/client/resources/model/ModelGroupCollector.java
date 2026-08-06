/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public class ModelGroupCollector {
    static final int SINGLETON_MODEL_GROUP = -1;
    private static final int INVISIBLE_MODEL_GROUP = 0;

    public static Object2IntMap<BlockState> build(BlockColors blockColors, BlockStateModelLoader.LoadedModels input) {
        HashMap coloringPropertiesCache = new HashMap();
        HashMap modelGroups = new HashMap();
        input.models().forEach((state, loadedModel) -> {
            List coloringProperties = coloringPropertiesCache.computeIfAbsent(state.getBlock(), block -> List.copyOf(blockColors.getColoringProperties((Block)block)));
            GroupKey key = GroupKey.create(state, loadedModel, coloringProperties);
            modelGroups.computeIfAbsent(key, k -> Sets.newIdentityHashSet()).add(state);
        });
        int nextModelGroup = 1;
        Object2IntOpenHashMap result = new Object2IntOpenHashMap();
        result.defaultReturnValue(-1);
        for (Set states : modelGroups.values()) {
            Iterator it = states.iterator();
            while (it.hasNext()) {
                BlockState state2 = (BlockState)it.next();
                if (state2.getRenderShape() == RenderShape.MODEL) continue;
                it.remove();
                result.put((Object)state2, 0);
            }
            if (states.size() <= 1) continue;
            int modelGroup = nextModelGroup++;
            states.forEach(arg_0 -> ModelGroupCollector.lambda$build$3((Object2IntMap)result, modelGroup, arg_0));
        }
        return result;
    }

    private static /* synthetic */ void lambda$build$3(Object2IntMap result, int modelGroup, BlockState blockState) {
        result.put((Object)blockState, modelGroup);
    }

    @Environment(value=EnvType.CLIENT)
    private record GroupKey(Object equalityGroup, List<Object> coloringValues) {
        public static GroupKey create(BlockState state, BlockStateModel.UnbakedRoot model, List<Property<?>> coloringProperties) {
            List<Object> coloringValues = GroupKey.getColoringValues(state, coloringProperties);
            Object equalityGroup = model.visualEqualityGroup(state);
            return new GroupKey(equalityGroup, coloringValues);
        }

        private static List<Object> getColoringValues(BlockState state, List<Property<?>> coloringProperties) {
            Object[] coloringValues = new Object[coloringProperties.size()];
            for (int i = 0; i < coloringProperties.size(); ++i) {
                coloringValues[i] = state.getValue(coloringProperties.get(i));
            }
            return List.of(coloringValues);
        }
    }
}

