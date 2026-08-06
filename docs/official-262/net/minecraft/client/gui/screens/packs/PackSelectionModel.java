/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Lists
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

@Environment(value=EnvType.CLIENT)
public class PackSelectionModel {
    private final PackRepository repository;
    private final List<Pack> selected;
    private final List<Pack> unselected;
    private final Function<Pack, Identifier> iconGetter;
    private final Consumer<EntryBase> onListChanged;
    private final Consumer<PackRepository> output;

    public PackSelectionModel(Consumer<EntryBase> onListChanged, Function<Pack, Identifier> iconGetter, PackRepository repository, Consumer<PackRepository> output) {
        this.onListChanged = onListChanged;
        this.iconGetter = iconGetter;
        this.repository = repository;
        this.selected = Lists.newArrayList(repository.getSelectedPacks());
        Collections.reverse(this.selected);
        this.unselected = Lists.newArrayList(repository.getAvailablePacks());
        this.unselected.removeAll(this.selected);
        this.output = output;
    }

    public Stream<Entry> getUnselected() {
        return this.unselected.stream().map(x$0 -> new UnselectedPackEntry(this, (Pack)x$0));
    }

    public Stream<Entry> getSelected() {
        return this.selected.stream().map(x$0 -> new SelectedPackEntry(this, (Pack)x$0));
    }

    private void updateRepoSelectedList() {
        this.repository.setSelected((Collection)Lists.reverse(this.selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
    }

    public void commit() {
        this.updateRepoSelectedList();
        this.output.accept(this.repository);
    }

    public void findNewPacks() {
        this.repository.reload();
        this.selected.retainAll(this.repository.getAvailablePacks());
        this.unselected.clear();
        this.unselected.addAll(this.repository.getAvailablePacks());
        this.unselected.removeAll(this.selected);
    }

    @Environment(value=EnvType.CLIENT)
    private class SelectedPackEntry
    extends EntryBase {
        final /* synthetic */ PackSelectionModel this$0;

        public SelectedPackEntry(PackSelectionModel packSelectionModel, Pack pack) {
            PackSelectionModel packSelectionModel2 = packSelectionModel;
            Objects.requireNonNull(packSelectionModel2);
            this.this$0 = packSelectionModel2;
            super(packSelectionModel, pack);
        }

        @Override
        protected List<Pack> getSelfList() {
            return this.this$0.selected;
        }

        @Override
        protected List<Pack> getOtherList() {
            return this.this$0.unselected;
        }

        @Override
        public boolean isSelected() {
            return true;
        }

        @Override
        public void select() {
        }

        @Override
        public void unselect() {
            this.toggleSelection();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class UnselectedPackEntry
    extends EntryBase {
        final /* synthetic */ PackSelectionModel this$0;

        public UnselectedPackEntry(PackSelectionModel packSelectionModel, Pack pack) {
            PackSelectionModel packSelectionModel2 = packSelectionModel;
            Objects.requireNonNull(packSelectionModel2);
            this.this$0 = packSelectionModel2;
            super(packSelectionModel, pack);
        }

        @Override
        protected List<Pack> getSelfList() {
            return this.this$0.unselected;
        }

        @Override
        protected List<Pack> getOtherList() {
            return this.this$0.selected;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void select() {
            this.toggleSelection();
        }

        @Override
        public void unselect() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    public abstract class EntryBase
    implements Entry {
        private final Pack pack;
        final /* synthetic */ PackSelectionModel this$0;

        public EntryBase(PackSelectionModel this$0, Pack pack) {
            PackSelectionModel packSelectionModel = this$0;
            Objects.requireNonNull(packSelectionModel);
            this.this$0 = packSelectionModel;
            this.pack = pack;
        }

        protected abstract List<Pack> getSelfList();

        protected abstract List<Pack> getOtherList();

        @Override
        public Identifier getIconTexture() {
            return this.this$0.iconGetter.apply(this.pack);
        }

        @Override
        public PackCompatibility getCompatibility() {
            return this.pack.getCompatibility();
        }

        @Override
        public String getId() {
            return this.pack.getId();
        }

        @Override
        public Component getTitle() {
            return this.pack.getTitle();
        }

        @Override
        public Component getDescription() {
            return this.pack.getDescription();
        }

        @Override
        public PackSource getPackSource() {
            return this.pack.getPackSource();
        }

        @Override
        public boolean isFixedPosition() {
            return this.pack.isFixedPosition();
        }

        @Override
        public boolean isRequired() {
            return this.pack.isRequired();
        }

        protected void toggleSelection() {
            this.getSelfList().remove(this.pack);
            this.pack.getDefaultPosition().insert(this.getOtherList(), this.pack, Pack::selectionConfig, true);
            this.this$0.onListChanged.accept(this);
            this.this$0.updateRepoSelectedList();
            this.updateHighContrastOptionInstance();
        }

        private void updateHighContrastOptionInstance() {
            if (this.pack.getId().equals("high_contrast")) {
                OptionInstance<Boolean> highContrastMode;
                highContrastMode.set((highContrastMode = Minecraft.getInstance().options.highContrast()).get() == false);
            }
        }

        protected void move(int direction) {
            List<Pack> list = this.getSelfList();
            int currentPos = list.indexOf(this.pack);
            list.remove(currentPos);
            list.add(currentPos + direction, this.pack);
            this.this$0.onListChanged.accept(this);
        }

        @Override
        public boolean canMoveUp() {
            List<Pack> list = this.getSelfList();
            int index = list.indexOf(this.pack);
            return index > 0 && !list.get(index - 1).isFixedPosition();
        }

        @Override
        public void moveUp() {
            this.move(-1);
        }

        @Override
        public boolean canMoveDown() {
            List<Pack> list = this.getSelfList();
            int index = list.indexOf(this.pack);
            return index >= 0 && index < list.size() - 1 && !list.get(index + 1).isFixedPosition();
        }

        @Override
        public void moveDown() {
            this.move(1);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Entry {
        public Identifier getIconTexture();

        public PackCompatibility getCompatibility();

        public String getId();

        public Component getTitle();

        public Component getDescription();

        public PackSource getPackSource();

        default public Component getExtendedDescription() {
            return this.getPackSource().decorate(this.getDescription());
        }

        public boolean isFixedPosition();

        public boolean isRequired();

        public void select();

        public void unselect();

        public void moveUp();

        public void moveDown();

        public boolean isSelected();

        default public boolean canSelect() {
            return !this.isSelected();
        }

        default public boolean canUnselect() {
            return this.isSelected() && !this.isRequired();
        }

        public boolean canMoveUp();

        public boolean canMoveDown();
    }
}

