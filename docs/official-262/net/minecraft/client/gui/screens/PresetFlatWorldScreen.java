/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Splitter
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class PresetFlatWorldScreen
extends Screen {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SLOT_BG_SIZE = 18;
    private static final int SLOT_STAT_HEIGHT = 20;
    private static final int SLOT_BG_X = 1;
    private static final int SLOT_BG_Y = 1;
    private static final int SLOT_FG_X = 2;
    private static final int SLOT_FG_Y = 2;
    private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
    public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
    private final CreateFlatWorldScreen parent;
    private Component shareText;
    private Component listText;
    private PresetsList list;
    private Button selectButton;
    private EditBox export;
    private FlatLevelGeneratorSettings settings;

    public PresetFlatWorldScreen(CreateFlatWorldScreen parent) {
        super(Component.translatable("createWorld.customize.presets.title"));
        this.parent = parent;
    }

    private static @Nullable FlatLayerInfo getLayerInfoFromString(HolderGetter<Block> blocks, String input, int firstFree) {
        Optional<Holder.Reference<Block>> block;
        int height;
        String blockId;
        List parts = Splitter.on((char)'*').limit(2).splitToList((CharSequence)input);
        if (parts.size() == 2) {
            blockId = (String)parts.get(1);
            try {
                height = Math.max(Integer.parseInt((String)parts.get(0)), 0);
            }
            catch (NumberFormatException e) {
                LOGGER.error("Error while parsing flat world string", (Throwable)e);
                return null;
            }
        } else {
            blockId = (String)parts.get(0);
            height = 1;
        }
        int firstAbove = Math.min(firstFree + height, DimensionType.Y_SIZE);
        int actualHeight = firstAbove - firstFree;
        try {
            block = blocks.get(ResourceKey.create(Registries.BLOCK, Identifier.parse(blockId)));
        }
        catch (Exception e) {
            LOGGER.error("Error while parsing flat world string", (Throwable)e);
            return null;
        }
        if (block.isEmpty()) {
            LOGGER.error("Error while parsing flat world string => Unknown block, {}", (Object)blockId);
            return null;
        }
        return new FlatLayerInfo(actualHeight, (Holder<Block>)block.get());
    }

    private static List<FlatLayerInfo> getLayersInfoFromString(HolderGetter<Block> blocks, String input) {
        ArrayList result = Lists.newArrayList();
        String[] depths = input.split(",");
        int firstFree = 0;
        for (String depth : depths) {
            FlatLayerInfo layer = PresetFlatWorldScreen.getLayerInfoFromString(blocks, depth, firstFree);
            if (layer == null) {
                return Collections.emptyList();
            }
            int maxHeight = DimensionType.Y_SIZE - firstFree;
            if (maxHeight <= 0) continue;
            result.add(layer.heightLimited(maxHeight));
            firstFree += layer.getHeight();
        }
        return result;
    }

    public static FlatLevelGeneratorSettings fromString(HolderGetter<Block> blocks, HolderGetter<Biome> biomes, HolderGetter<StructureSet> structureSets, HolderGetter<PlacedFeature> placedFeatures, String definition, FlatLevelGeneratorSettings settings) {
        Holder.Reference<Biome> defaultBiome;
        Iterator parts = Splitter.on((char)';').split((CharSequence)definition).iterator();
        if (!parts.hasNext()) {
            return FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures);
        }
        List<FlatLayerInfo> layers = PresetFlatWorldScreen.getLayersInfoFromString(blocks, (String)parts.next());
        if (layers.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures);
        }
        Holder<Biome> biome = defaultBiome = biomes.getOrThrow(DEFAULT_BIOME);
        if (parts.hasNext()) {
            String biomeName = (String)parts.next();
            biome = Optional.ofNullable(Identifier.tryParse(biomeName)).map(id -> ResourceKey.create(Registries.BIOME, id)).flatMap(biomes::get).orElseGet(() -> {
                LOGGER.warn("Invalid biome: {}", (Object)biomeName);
                return defaultBiome;
            });
        }
        return settings.withBiomeAndLayers(layers, settings.structureOverrides(), biome);
    }

    private static String save(FlatLevelGeneratorSettings settings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < settings.getLayersInfo().size(); ++i) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(settings.getLayersInfo().get(i));
        }
        builder.append(";");
        builder.append(settings.getBiome().unwrapKey().map(ResourceKey::identifier).orElseThrow(() -> new IllegalStateException("Biome not registered")));
        return builder.toString();
    }

    @Override
    protected void init() {
        this.shareText = Component.translatable("createWorld.customize.presets.share");
        this.listText = Component.translatable("createWorld.customize.presets.list");
        this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
        this.export.setMaxLength(1230);
        WorldCreationContext worldCreatingContext = this.parent.parent.getUiState().getSettings();
        RegistryAccess.Frozen registryAccess = worldCreatingContext.worldgenLoadContext();
        FeatureFlagSet enabledFeatures = worldCreatingContext.dataConfiguration().enabledFeatures();
        HolderLookup.RegistryLookup biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderLookup.RegistryLookup structureSets = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderLookup.RegistryLookup placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        HolderLookup.RegistryLookup blocks = registryAccess.lookupOrThrow(Registries.BLOCK).filterFeatures(enabledFeatures);
        this.export.setValue(PresetFlatWorldScreen.save(this.parent.settings()));
        this.settings = this.parent.settings();
        this.addWidget(this.export);
        this.list = this.addRenderableWidget(new PresetsList(this, registryAccess, enabledFeatures));
        this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets.select"), button -> {
            FlatLevelGeneratorSettings generator = PresetFlatWorldScreen.fromString(blocks, biomes, structureSets, placedFeatures, this.export.getValue(), this.settings);
            this.parent.setConfig(generator);
            this.minecraft.gui.setScreen(this.parent);
        }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.gui.setScreen(this.parent)).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
        this.updateButtonValidity(this.list.getSelected() != null);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return this.list.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void resize(int width, int height) {
        String oldEdit = this.export.getValue();
        this.init(width, height);
        this.export.setValue(oldEdit);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, -1);
        graphics.text(this.font, this.shareText, 51, 30, -6250336);
        graphics.text(this.font, this.listText, 51, 68, -6250336);
        this.export.extractRenderState(graphics, mouseX, mouseY, a);
    }

    public void updateButtonValidity(boolean hasSelected) {
        this.selectButton.active = hasSelected || this.export.getValue().length() > 1;
    }

    @Environment(value=EnvType.CLIENT)
    private class PresetsList
    extends ObjectSelectionList<Entry> {
        final /* synthetic */ PresetFlatWorldScreen this$0;

        public PresetsList(PresetFlatWorldScreen presetFlatWorldScreen, RegistryAccess access, FeatureFlagSet enabledFeatures) {
            PresetFlatWorldScreen presetFlatWorldScreen2 = presetFlatWorldScreen;
            Objects.requireNonNull(presetFlatWorldScreen2);
            this.this$0 = presetFlatWorldScreen2;
            super(presetFlatWorldScreen.minecraft, presetFlatWorldScreen.width, presetFlatWorldScreen.height - 117, 80, 24);
            for (Holder<FlatLevelGeneratorPreset> preset : access.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET).getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
                Set disabledBlocks = preset.value().settings().getLayersInfo().stream().map(p -> p.getBlockState().getBlock()).filter(b -> !b.isEnabled(enabledFeatures)).collect(Collectors.toSet());
                if (!disabledBlocks.isEmpty()) {
                    LOGGER.info("Discarding flat world preset {} since it contains experimental blocks {}", (Object)preset.unwrapKey().map(e -> e.identifier().toString()).orElse("<unknown>"), disabledBlocks);
                    continue;
                }
                this.addEntry(new Entry(this, preset));
            }
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            this.this$0.updateButtonValidity(selected != null);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (super.keyPressed(event)) {
                return true;
            }
            if (event.isSelection() && this.getSelected() != null) {
                ((Entry)this.getSelected()).select();
            }
            return false;
        }

        @Environment(value=EnvType.CLIENT)
        public class Entry
        extends ObjectSelectionList.Entry<Entry> {
            private final FlatLevelGeneratorPreset preset;
            private final Component name;
            final /* synthetic */ PresetsList this$1;

            public Entry(PresetsList this$1, Holder<FlatLevelGeneratorPreset> preset) {
                PresetsList presetsList = this$1;
                Objects.requireNonNull(presetsList);
                this.this$1 = presetsList;
                this.preset = preset.value();
                this.name = preset.unwrapKey().map(key -> Component.translatable(key.identifier().toLanguageKey("flat_world_preset"))).orElse(UNKNOWN_PRESET);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                this.blitSlot(graphics, this.getContentX(), this.getContentY(), this.preset.displayItem().value());
                graphics.text(this.this$1.this$0.font, this.name, this.getContentX() + 18 + 5, this.getContentY() + 6, -1);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.select();
                return super.mouseClicked(event, doubleClick);
            }

            private void select() {
                this.this$1.setSelected(this);
                this.this$1.this$0.settings = this.preset.settings();
                this.this$1.this$0.export.setValue(PresetFlatWorldScreen.save(this.this$1.this$0.settings));
                this.this$1.this$0.export.moveCursorToStart(false);
            }

            private void blitSlot(GuiGraphicsExtractor graphics, int x, int y, Item item) {
                this.blitSlotBg(graphics, x + 1, y + 1);
                graphics.fakeItem(new ItemStack(item), x + 2, y + 2);
            }

            private void blitSlotBg(GuiGraphicsExtractor graphics, int x, int y) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, 18, 18);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }
        }
    }
}

