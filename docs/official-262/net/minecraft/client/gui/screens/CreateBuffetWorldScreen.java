/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.ibm.icu.text.Collator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens;

import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class CreateBuffetWorldScreen
extends Screen {
    private static final Component SEARCH_HINT = Component.translatable("createWorld.customize.buffet.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SPACING = 3;
    private static final int SEARCH_BOX_HEIGHT = 15;
    private final HeaderAndFooterLayout layout;
    private final Screen parent;
    private final Consumer<Holder<Biome>> applySettings;
    private final Registry<Biome> biomes;
    private BiomeList list;
    private Holder<Biome> biome;
    private Button doneButton;

    public CreateBuffetWorldScreen(Screen parent, WorldCreationContext settings, Consumer<Holder<Biome>> applySettings) {
        super(Component.translatable("createWorld.customize.buffet.title"));
        this.parent = parent;
        this.applySettings = applySettings;
        this.layout = new HeaderAndFooterLayout(this, 13 + this.font.lineHeight + 3 + 15, 33);
        this.biomes = settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
        Holder defaultBiome = (Holder)this.biomes.get(Biomes.PLAINS).or(() -> this.biomes.listElements().findAny()).orElseThrow();
        this.biome = settings.selectedDimensions().overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse(defaultBiome);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(3));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        EditBox search = header.addChild(new EditBox(this.font, 200, 15, Component.empty()));
        BiomeList biomeList = new BiomeList(this);
        search.setHint(SEARCH_HINT);
        search.setResponder(biomeList::filterEntries);
        this.list = this.layout.addToContents(biomeList);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.applySettings.accept(this.biome);
            this.onClose();
        }).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
        this.list.setSelected((BiomeList.Entry)this.list.children().stream().filter(e -> Objects.equals(e.biome, this.biome)).findFirst().orElse(null));
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.list.updateSize(this.width, this.layout);
    }

    private void updateButtonValidity() {
        this.doneButton.active = this.list.getSelected() != null;
    }

    @Environment(value=EnvType.CLIENT)
    private class BiomeList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ CreateBuffetWorldScreen this$0;

        private BiomeList(CreateBuffetWorldScreen createBuffetWorldScreen) {
            CreateBuffetWorldScreen createBuffetWorldScreen2 = createBuffetWorldScreen;
            Objects.requireNonNull(createBuffetWorldScreen2);
            this.this$0 = createBuffetWorldScreen2;
            super(createBuffetWorldScreen.minecraft, createBuffetWorldScreen.width, createBuffetWorldScreen.layout.getContentHeight(), createBuffetWorldScreen.layout.getHeaderHeight(), 15);
            this.filterEntries("");
        }

        private void filterEntries(String filter) {
            Collator localeCollator = Collator.getInstance((Locale)Locale.getDefault());
            String lowercaseFilter = filter.toLowerCase(Locale.ROOT);
            List<Entry> list = this.this$0.biomes.listElements().map(x$0 -> new Entry(this, (Holder.Reference<Biome>)x$0)).sorted(Comparator.comparing(e -> e.name.getString(), localeCollator)).filter(entry -> filter.isEmpty() || entry.name.getString().toLowerCase(Locale.ROOT).contains(lowercaseFilter)).toList();
            this.replaceEntries(list);
            this.refreshScrollAmount();
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                this.this$0.biome = selected.biome;
            }
            this.this$0.updateButtonValidity();
        }

        @Environment(value=EnvType.CLIENT)
        private class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private final Holder.Reference<Biome> biome;
            private final Component name;
            final /* synthetic */ BiomeList this$1;

            public Entry(BiomeList biomeList, Holder.Reference<Biome> biome) {
                BiomeList biomeList2 = biomeList;
                Objects.requireNonNull(biomeList2);
                this.this$1 = biomeList2;
                this.biome = biome;
                Identifier id = biome.key().identifier();
                String translationKey = id.toLanguageKey("biome");
                this.name = Language.getInstance().has(translationKey) ? Component.translatable(translationKey) : Component.literal(id.toString());
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.text(this.this$1.this$0.font, this.name, this.getContentX() + 5, this.getContentY() + 2, -1);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.this$1.setSelected(this);
                return super.mouseClicked(event, doubleClick);
            }
        }
    }
}

