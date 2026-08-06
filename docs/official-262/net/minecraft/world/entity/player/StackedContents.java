/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  it.unimi.dsi.fastutil.ints.IntArrayList
 *  it.unimi.dsi.fastutil.ints.IntList
 *  it.unimi.dsi.fastutil.objects.ObjectIterable
 *  it.unimi.dsi.fastutil.objects.Reference2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Reference2IntMaps
 *  it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.player;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class StackedContents<T> {
    public final Reference2IntOpenHashMap<T> amounts = new Reference2IntOpenHashMap();

    private boolean hasAtLeast(T item, int count) {
        return this.amounts.getInt(item) >= count;
    }

    private void take(T item, int amount) {
        int previous = this.amounts.addTo(item, -amount);
        if (previous < amount) {
            throw new IllegalStateException("Took " + amount + " items, but only had " + previous);
        }
    }

    private void put(T item, int count) {
        this.amounts.addTo(item, count);
    }

    public boolean tryPick(List<? extends IngredientInfo<T>> ingredients, int amount, @Nullable Output<T> output) {
        return new RecipePicker(this, ingredients).tryPick(amount, output);
    }

    public int tryPickAll(List<? extends IngredientInfo<T>> ingredients, int maxSize, @Nullable Output<T> output) {
        return new RecipePicker(this, ingredients).tryPickAll(maxSize, output);
    }

    public void clear() {
        this.amounts.clear();
    }

    public void account(T item, int count) {
        this.put(item, count);
    }

    private List<T> getUniqueAvailableIngredientItems(Iterable<? extends IngredientInfo<T>> ingredients) {
        ArrayList<Object> result = new ArrayList<Object>();
        for (Reference2IntMap.Entry availableItem : Reference2IntMaps.fastIterable(this.amounts)) {
            if (availableItem.getIntValue() <= 0 || !StackedContents.anyIngredientMatches(ingredients, availableItem.getKey())) continue;
            result.add(availableItem.getKey());
        }
        return result;
    }

    private static <T> boolean anyIngredientMatches(Iterable<? extends IngredientInfo<T>> ingredients, T item) {
        for (IngredientInfo<T> ingredient : ingredients) {
            if (!ingredient.acceptsItem(item)) continue;
            return true;
        }
        return false;
    }

    @VisibleForTesting
    public int getResultUpperBound(List<? extends IngredientInfo<T>> ingredients) {
        int min = Integer.MAX_VALUE;
        ObjectIterable availableItems = Reference2IntMaps.fastIterable(this.amounts);
        block0: for (IngredientInfo<Object> ingredientInfo : ingredients) {
            int max = 0;
            for (Reference2IntMap.Entry entry : availableItems) {
                int itemCount = entry.getIntValue();
                if (itemCount <= max) continue;
                if (ingredientInfo.acceptsItem(entry.getKey())) {
                    max = itemCount;
                }
                if (max < min) continue;
                continue block0;
            }
            min = max;
            if (min != 0) continue;
            break;
        }
        return min;
    }

    private class RecipePicker {
        private final List<? extends IngredientInfo<T>> ingredients;
        private final int ingredientCount;
        private final List<T> items;
        private final int itemCount;
        private final BitSet data;
        private final IntList path;
        final /* synthetic */ StackedContents this$0;

        public RecipePicker(StackedContents stackedContents, List<? extends IngredientInfo<T>> ingredients) {
            StackedContents stackedContents2 = stackedContents;
            Objects.requireNonNull(stackedContents2);
            this.this$0 = stackedContents2;
            this.path = new IntArrayList();
            this.ingredients = ingredients;
            this.ingredientCount = ingredients.size();
            this.items = stackedContents.getUniqueAvailableIngredientItems(ingredients);
            this.itemCount = this.items.size();
            this.data = new BitSet(this.visitedIngredientCount() + this.visitedItemCount() + this.satisfiedCount() + this.connectionCount() + this.residualCount());
            this.setInitialConnections();
        }

        private void setInitialConnections() {
            for (int ingredient = 0; ingredient < this.ingredientCount; ++ingredient) {
                IngredientInfo ingredientInfo = this.ingredients.get(ingredient);
                for (int item = 0; item < this.itemCount; ++item) {
                    if (!ingredientInfo.acceptsItem(this.items.get(item))) continue;
                    this.setConnection(item, ingredient);
                }
            }
        }

        public boolean tryPick(int capacity, @Nullable Output<T> output) {
            IntList path;
            if (capacity <= 0) {
                return true;
            }
            int satisfiedIngredientCount = 0;
            while ((path = this.tryAssigningNewItem(capacity)) != null) {
                int assignedItem = path.getInt(0);
                this.this$0.take(this.items.get(assignedItem), capacity);
                int satisfiedIngredient = path.size() - 1;
                this.setSatisfied(path.getInt(satisfiedIngredient));
                ++satisfiedIngredientCount;
                for (int i = 0; i < path.size() - 1; ++i) {
                    int ingredient;
                    int item;
                    if (RecipePicker.isPathIndexItem(i)) {
                        item = path.getInt(i);
                        ingredient = path.getInt(i + 1);
                        this.assign(item, ingredient);
                        continue;
                    }
                    item = path.getInt(i + 1);
                    ingredient = path.getInt(i);
                    this.unassign(item, ingredient);
                }
            }
            boolean isValidAssignment = satisfiedIngredientCount == this.ingredientCount;
            boolean hasOutput = isValidAssignment && output != null;
            this.clearAllVisited();
            this.clearSatisfied();
            block2: for (int ingredient = 0; ingredient < this.ingredientCount; ++ingredient) {
                for (int item = 0; item < this.itemCount; ++item) {
                    if (!this.isAssigned(item, ingredient)) continue;
                    this.unassign(item, ingredient);
                    this.this$0.put(this.items.get(item), capacity);
                    if (!hasOutput) continue block2;
                    output.accept(this.items.get(item));
                    continue block2;
                }
            }
            assert (this.data.get(this.residualOffset(), this.residualOffset() + this.residualCount()).isEmpty());
            return isValidAssignment;
        }

        private static boolean isPathIndexItem(int index) {
            return (index & 1) == 0;
        }

        private @Nullable IntList tryAssigningNewItem(int capacity) {
            this.clearAllVisited();
            for (int item = 0; item < this.itemCount; ++item) {
                IntList path;
                if (!this.this$0.hasAtLeast(this.items.get(item), capacity) || (path = this.findNewItemAssignmentPath(item)) == null) continue;
                return path;
            }
            return null;
        }

        private @Nullable IntList findNewItemAssignmentPath(int startingItem) {
            this.path.clear();
            this.visitItem(startingItem);
            this.path.add(startingItem);
            while (!this.path.isEmpty()) {
                int newLength;
                int pathLength = this.path.size();
                if (RecipePicker.isPathIndexItem(pathLength - 1)) {
                    int itemToAssign = this.path.getInt(pathLength - 1);
                    for (int ingredient = 0; ingredient < this.ingredientCount; ++ingredient) {
                        if (this.hasVisitedIngredient(ingredient) || !this.hasConnection(itemToAssign, ingredient) || this.isAssigned(itemToAssign, ingredient)) continue;
                        this.visitIngredient(ingredient);
                        this.path.add(ingredient);
                        break;
                    }
                } else {
                    int lastAssignedIngredient = this.path.getInt(pathLength - 1);
                    if (!this.isSatisfied(lastAssignedIngredient)) {
                        return this.path;
                    }
                    for (int item = 0; item < this.itemCount; ++item) {
                        if (this.hasVisitedItem(item) || !this.isAssigned(item, lastAssignedIngredient)) continue;
                        assert (this.hasConnection(item, lastAssignedIngredient));
                        this.visitItem(item);
                        this.path.add(item);
                        break;
                    }
                }
                if ((newLength = this.path.size()) != pathLength) continue;
                this.path.removeInt(newLength - 1);
            }
            return null;
        }

        private int visitedIngredientOffset() {
            return 0;
        }

        private int visitedIngredientCount() {
            return this.ingredientCount;
        }

        private int visitedItemOffset() {
            return this.visitedIngredientOffset() + this.visitedIngredientCount();
        }

        private int visitedItemCount() {
            return this.itemCount;
        }

        private int satisfiedOffset() {
            return this.visitedItemOffset() + this.visitedItemCount();
        }

        private int satisfiedCount() {
            return this.ingredientCount;
        }

        private int connectionOffset() {
            return this.satisfiedOffset() + this.satisfiedCount();
        }

        private int connectionCount() {
            return this.ingredientCount * this.itemCount;
        }

        private int residualOffset() {
            return this.connectionOffset() + this.connectionCount();
        }

        private int residualCount() {
            return this.ingredientCount * this.itemCount;
        }

        private boolean isSatisfied(int ingredient) {
            return this.data.get(this.getSatisfiedIndex(ingredient));
        }

        private void setSatisfied(int ingredient) {
            this.data.set(this.getSatisfiedIndex(ingredient));
        }

        private int getSatisfiedIndex(int ingredient) {
            assert (ingredient >= 0 && ingredient < this.ingredientCount);
            return this.satisfiedOffset() + ingredient;
        }

        private void clearSatisfied() {
            this.clearRange(this.satisfiedOffset(), this.satisfiedCount());
        }

        private void setConnection(int item, int ingredient) {
            this.data.set(this.getConnectionIndex(item, ingredient));
        }

        private boolean hasConnection(int item, int ingredient) {
            return this.data.get(this.getConnectionIndex(item, ingredient));
        }

        private int getConnectionIndex(int item, int ingredient) {
            assert (item >= 0 && item < this.itemCount);
            assert (ingredient >= 0 && ingredient < this.ingredientCount);
            return this.connectionOffset() + item * this.ingredientCount + ingredient;
        }

        private boolean isAssigned(int item, int ingredient) {
            return this.data.get(this.getResidualIndex(item, ingredient));
        }

        private void assign(int item, int ingredient) {
            int residualIndex = this.getResidualIndex(item, ingredient);
            assert (!this.data.get(residualIndex));
            this.data.set(residualIndex);
        }

        private void unassign(int item, int ingredient) {
            int residualIndex = this.getResidualIndex(item, ingredient);
            assert (this.data.get(residualIndex));
            this.data.clear(residualIndex);
        }

        private int getResidualIndex(int item, int ingredient) {
            assert (item >= 0 && item < this.itemCount);
            assert (ingredient >= 0 && ingredient < this.ingredientCount);
            return this.residualOffset() + item * this.ingredientCount + ingredient;
        }

        private void visitIngredient(int item) {
            this.data.set(this.getVisitedIngredientIndex(item));
        }

        private boolean hasVisitedIngredient(int ingredient) {
            return this.data.get(this.getVisitedIngredientIndex(ingredient));
        }

        private int getVisitedIngredientIndex(int ingredient) {
            assert (ingredient >= 0 && ingredient < this.ingredientCount);
            return this.visitedIngredientOffset() + ingredient;
        }

        private void visitItem(int item) {
            this.data.set(this.getVisitiedItemIndex(item));
        }

        private boolean hasVisitedItem(int item) {
            return this.data.get(this.getVisitiedItemIndex(item));
        }

        private int getVisitiedItemIndex(int item) {
            assert (item >= 0 && item < this.itemCount);
            return this.visitedItemOffset() + item;
        }

        private void clearAllVisited() {
            this.clearRange(this.visitedIngredientOffset(), this.visitedIngredientCount());
            this.clearRange(this.visitedItemOffset(), this.visitedItemCount());
        }

        private void clearRange(int offset, int count) {
            this.data.clear(offset, offset + count);
        }

        public int tryPickAll(int maxSize, @Nullable Output<T> output) {
            int mid;
            int min = 0;
            int max = Math.min(maxSize, this.this$0.getResultUpperBound(this.ingredients)) + 1;
            while (true) {
                if (this.tryPick(mid = (min + max) / 2, null)) {
                    if (max - min <= 1) break;
                    min = mid;
                    continue;
                }
                max = mid;
            }
            if (mid > 0) {
                this.tryPick(mid, output);
            }
            return mid;
        }
    }

    @FunctionalInterface
    public static interface Output<T> {
        public void accept(T var1);
    }

    @FunctionalInterface
    public static interface IngredientInfo<T> {
        public boolean acceptsItem(T var1);
    }
}

