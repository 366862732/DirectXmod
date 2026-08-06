/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class WorldSelectionList
extends ObjectSelectionList<Entry> {
    public static final DateTimeFormatter DATE_FORMAT = Util.localizedDateFormatter(FormatStyle.SHORT);
    private static final Identifier ERROR_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/error_highlighted");
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("world_list/error");
    private static final Identifier MARKED_JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join_highlighted");
    private static final Identifier MARKED_JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/marked_join");
    private static final Identifier WARNING_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/warning_highlighted");
    private static final Identifier WARNING_SPRITE = Identifier.withDefaultNamespace("world_list/warning");
    private static final Identifier JOIN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("world_list/join_highlighted");
    private static final Identifier JOIN_SPRITE = Identifier.withDefaultNamespace("world_list/join");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    private static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    private static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    private static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    private static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
    private static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    private static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
    private static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
    private final Screen screen;
    private CompletableFuture<List<LevelSummary>> pendingLevels;
    private @Nullable List<LevelSummary> currentlyDisplayedLevels;
    private final LoadingHeader loadingHeader;
    private final EntryType entryType;
    private String filter;
    private boolean hasPolled;
    private final @Nullable Consumer<LevelSummary> onEntrySelect;
    private final @Nullable Consumer<WorldListEntry> onEntryInteract;

    private WorldSelectionList(Screen screen, Minecraft minecraft, int width, int height, String filter, @Nullable WorldSelectionList oldList, @Nullable Consumer<LevelSummary> onEntrySelect, @Nullable Consumer<WorldListEntry> onEntryInteract, EntryType entryType) {
        super(minecraft, width, height, 0, 36);
        this.screen = screen;
        this.loadingHeader = new LoadingHeader(minecraft);
        this.filter = filter;
        this.onEntrySelect = onEntrySelect;
        this.onEntryInteract = onEntryInteract;
        this.entryType = entryType;
        this.pendingLevels = oldList != null ? oldList.pendingLevels : this.loadLevels();
        this.addEntry(this.loadingHeader);
        this.handleNewLevels(this.pollLevelsIgnoreErrors());
    }

    @Override
    protected void clearEntries() {
        this.children().forEach(Entry::close);
        super.clearEntries();
    }

    private @Nullable List<LevelSummary> pollLevelsIgnoreErrors() {
        try {
            List<LevelSummary> completedLevels = this.pendingLevels.getNow(null);
            if (this.entryType == EntryType.UPLOAD_WORLD) {
                if (completedLevels != null && !this.hasPolled) {
                    this.hasPolled = true;
                    completedLevels = completedLevels.stream().filter(LevelSummary::canUpload).toList();
                } else {
                    return null;
                }
            }
            return completedLevels;
        }
        catch (CancellationException | CompletionException ignore) {
            return null;
        }
    }

    public void reloadWorldList() {
        this.pendingLevels = this.loadLevels();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        List<LevelSummary> newLevels = this.pollLevelsIgnoreErrors();
        if (newLevels != this.currentlyDisplayedLevels) {
            this.handleNewLevels(newLevels);
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    private void handleNewLevels(@Nullable List<LevelSummary> levels) {
        if (levels == null) {
            return;
        }
        if (levels.isEmpty()) {
            switch (this.entryType.ordinal()) {
                case 0: {
                    CreateWorldScreen.openFresh(this.minecraft, () -> this.minecraft.gui.setScreen(null));
                    break;
                }
                case 1: {
                    this.clearEntries();
                    this.addEntry(new NoWorldsEntry(Component.translatable("mco.upload.select.world.none"), this.screen.getFont()));
                }
            }
        } else {
            this.fillLevels(this.filter, levels);
            this.currentlyDisplayedLevels = levels;
        }
    }

    public void updateFilter(String newFilter) {
        if (this.currentlyDisplayedLevels != null && !newFilter.equals(this.filter)) {
            this.fillLevels(newFilter, this.currentlyDisplayedLevels);
        }
        this.filter = newFilter;
    }

    private CompletableFuture<List<LevelSummary>> loadLevels() {
        LevelStorageSource.LevelCandidates levelCandidates;
        try {
            levelCandidates = this.minecraft.getLevelSource().findLevelCandidates();
        }
        catch (LevelStorageException e) {
            LOGGER.error("Couldn't load level list", (Throwable)e);
            this.handleLevelLoadFailure(e.getMessageComponent());
            return CompletableFuture.failedFuture(e);
        }
        return this.minecraft.getLevelSource().loadLevelSummaries(levelCandidates).exceptionally(throwable -> {
            this.minecraft.delayCrash(CrashReport.forThrowable(throwable, "Couldn't load level list"));
            return List.of();
        });
    }

    private void fillLevels(String filter, List<LevelSummary> levels) {
        ArrayList<WorldListEntry> worldEntries = new ArrayList<WorldListEntry>();
        Optional<WorldListEntry> selectedOpt = this.getSelectedOpt();
        WorldListEntry entryToSelect = null;
        for (LevelSummary level2 : levels.stream().filter(level -> this.filterAccepts(filter.toLowerCase(Locale.ROOT), (LevelSummary)level)).toList()) {
            WorldListEntry worldListEntry = new WorldListEntry(this, this, level2);
            if (selectedOpt.isPresent() && selectedOpt.get().getLevelSummary().getLevelId().equals(worldListEntry.getLevelSummary().getLevelId())) {
                entryToSelect = worldListEntry;
            }
            worldEntries.add(worldListEntry);
        }
        this.removeEntries(this.children().stream().filter(entry -> !worldEntries.contains(entry)).toList());
        worldEntries.forEach(entry -> {
            if (!this.children().contains(entry)) {
                this.addEntry(entry);
            }
        });
        this.setSelected(entryToSelect);
        this.notifyListUpdated();
    }

    private boolean filterAccepts(String filter, LevelSummary level) {
        return level.getLevelName().toLowerCase(Locale.ROOT).contains(filter) || level.getLevelId().toLowerCase(Locale.ROOT).contains(filter);
    }

    private void notifyListUpdated() {
        this.refreshScrollAmount();
        this.screen.triggerImmediateNarration(true);
    }

    private void handleLevelLoadFailure(Component message) {
        this.minecraft.gui.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), message));
    }

    @Override
    public int getRowWidth() {
        return 270;
    }

    @Override
    public void setSelected(@Nullable Entry selected) {
        super.setSelected(selected);
        if (this.onEntrySelect != null) {
            LevelSummary levelSummary;
            if (selected instanceof WorldListEntry) {
                WorldListEntry entry = (WorldListEntry)selected;
                levelSummary = entry.summary;
            } else {
                levelSummary = null;
            }
            this.onEntrySelect.accept(levelSummary);
        }
    }

    public Optional<WorldListEntry> getSelectedOpt() {
        Entry selected = (Entry)this.getSelected();
        if (selected instanceof WorldListEntry) {
            WorldListEntry worldEntry = (WorldListEntry)selected;
            return Optional.of(worldEntry);
        }
        return Optional.empty();
    }

    public void returnToScreen() {
        this.reloadWorldList();
        this.minecraft.gui.setScreen(this.screen);
    }

    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        if (this.children().contains(this.loadingHeader)) {
            this.loadingHeader.updateNarration(output);
            return;
        }
        super.updateWidgetNarration(output);
    }

    @Environment(value=EnvType.CLIENT)
    public static class LoadingHeader
    extends Entry {
        private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
        private final Minecraft minecraft;

        public LoadingHeader(Minecraft minecraft) {
            this.minecraft = minecraft;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int labelX = (this.minecraft.gui.screen().width - this.minecraft.font.width(LOADING_LABEL)) / 2;
            int labelY = this.getContentY() + (this.getContentHeight() - this.minecraft.font.lineHeight) / 2;
            graphics.text(this.minecraft.font, LOADING_LABEL, labelX, labelY, -1);
            String dots = LoadingDotsText.get(Util.getMillis());
            int dotsX = (this.minecraft.gui.screen().width - this.minecraft.font.width(dots)) / 2;
            int dotsY = labelY + this.minecraft.font.lineHeight;
            graphics.text(this.minecraft.font, dots, dotsX, dotsY, -8355712);
        }

        @Override
        public Component getNarration() {
            return LOADING_LABEL;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum EntryType {
        SINGLEPLAYER,
        UPLOAD_WORLD;

    }

    @Environment(value=EnvType.CLIENT)
    public static final class NoWorldsEntry
    extends Entry {
        private final StringWidget stringWidget;

        public NoWorldsEntry(Component component, Font font) {
            this.stringWidget = new StringWidget(component, font);
        }

        @Override
        public Component getNarration() {
            return this.stringWidget.getMessage();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.stringWidget.setPosition(this.getContentXMiddle() - this.stringWidget.getWidth() / 2, this.getContentYMiddle() - this.stringWidget.getHeight() / 2);
            this.stringWidget.extractRenderState(graphics, mouseX, mouseY, a);
        }
    }

    @Environment(value=EnvType.CLIENT)
    public final class WorldListEntry
    extends Entry
    implements SelectableEntry {
        private static final int ICON_SIZE = 32;
        private final WorldSelectionList list;
        private final Minecraft minecraft;
        private final Screen screen;
        private final LevelSummary summary;
        private final FaviconTexture icon;
        private final StringWidget worldNameText;
        private final StringWidget idAndLastPlayedText;
        private final StringWidget infoText;
        private @Nullable Path iconFile;
        final /* synthetic */ WorldSelectionList this$0;

        public WorldListEntry(WorldSelectionList this$0, WorldSelectionList list, LevelSummary summary) {
            WorldSelectionList worldSelectionList = this$0;
            Objects.requireNonNull(worldSelectionList);
            this.this$0 = worldSelectionList;
            this.list = list;
            this.minecraft = list.minecraft;
            this.screen = list.getScreen();
            this.summary = summary;
            this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), summary.getLevelId());
            this.iconFile = summary.getIcon();
            int maxTextWidth = list.getRowWidth() - this.getTextX() - 2;
            MutableComponent worldNameComponent = Component.literal(summary.getLevelName());
            this.worldNameText = new StringWidget(worldNameComponent, this.minecraft.font);
            this.worldNameText.setMaxWidth(maxTextWidth);
            if (this.minecraft.font.width(worldNameComponent) > maxTextWidth) {
                this.worldNameText.setTooltip(Tooltip.create(worldNameComponent));
            }
            Object levelIdAndDate = summary.getLevelId();
            long lastPlayed = summary.getLastPlayed();
            if (lastPlayed != -1L) {
                ZonedDateTime lastPlayedTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastPlayed), ZoneId.systemDefault());
                levelIdAndDate = (String)levelIdAndDate + " (" + DATE_FORMAT.format(lastPlayedTime) + ")";
            }
            MutableComponent levelIdAndDateComponent = Component.literal((String)levelIdAndDate).withColor(-8355712);
            this.idAndLastPlayedText = new StringWidget(levelIdAndDateComponent, this.minecraft.font);
            this.idAndLastPlayedText.setMaxWidth(maxTextWidth);
            if (this.minecraft.font.width((String)levelIdAndDate) > maxTextWidth) {
                this.idAndLastPlayedText.setTooltip(Tooltip.create(levelIdAndDateComponent));
            }
            Component info = ComponentUtils.mergeStyles(summary.getInfo(), Style.EMPTY.withColor(-8355712));
            this.infoText = new StringWidget(info, this.minecraft.font);
            this.infoText.setMaxWidth(maxTextWidth);
            if (this.minecraft.font.width(info) > maxTextWidth) {
                this.infoText.setTooltip(Tooltip.create(info));
            }
            this.validateIconFile();
            this.loadIcon();
        }

        private void validateIconFile() {
            if (this.iconFile == null) {
                return;
            }
            try {
                BasicFileAttributes attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (attributes.isSymbolicLink()) {
                    List<ForbiddenSymlinkInfo> issues = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                    if (!issues.isEmpty()) {
                        LOGGER.warn("{}", (Object)ContentValidationException.getMessage(this.iconFile, issues));
                        this.iconFile = null;
                    } else {
                        attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, new LinkOption[0]);
                    }
                }
                if (!attributes.isRegularFile()) {
                    this.iconFile = null;
                }
            }
            catch (NoSuchFileException e) {
                this.iconFile = null;
            }
            catch (IOException e) {
                LOGGER.error("could not validate symlink", (Throwable)e);
                this.iconFile = null;
            }
        }

        @Override
        public Component getNarration() {
            MutableComponent entryNarration = Component.translatable("narrator.select.world_info", this.summary.getLevelName(), Component.translationArg(new Date(this.summary.getLastPlayed())), this.summary.getInfo());
            if (this.summary.isLocked()) {
                entryNarration = CommonComponents.joinForNarration(entryNarration, WORLD_LOCKED_TOOLTIP);
            }
            if (this.summary.isExperimental()) {
                entryNarration = CommonComponents.joinForNarration(entryNarration, WORLD_EXPERIMENTAL);
            }
            return Component.translatable("narrator.select", entryNarration);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int textX = this.getTextX();
            this.worldNameText.setPosition(textX, this.getContentY() + 1);
            this.worldNameText.extractRenderState(graphics, mouseX, mouseY, a);
            this.idAndLastPlayedText.setPosition(textX, this.getContentY() + this.minecraft.font.lineHeight + 3);
            this.idAndLastPlayedText.extractRenderState(graphics, mouseX, mouseY, a);
            this.infoText.setPosition(textX, this.getContentY() + this.minecraft.font.lineHeight + this.minecraft.font.lineHeight + 3);
            this.infoText.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getContentX(), this.getContentY(), 0.0f, 0.0f, 32, 32, 32, 32);
            if (this.list.entryType == EntryType.SINGLEPLAYER && hovered) {
                Identifier joinWithErrorSprite;
                graphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + 32, this.getContentY() + 32, -1601138544);
                int relX = mouseX - this.getContentX();
                int relY = mouseY - this.getContentY();
                boolean isOverIcon = this.mouseOverIcon(relX, relY, 32);
                Identifier joinSprite = isOverIcon ? JOIN_HIGHLIGHTED_SPRITE : JOIN_SPRITE;
                Identifier warningSprite = isOverIcon ? WARNING_HIGHLIGHTED_SPRITE : WARNING_SPRITE;
                Identifier errorSprite = isOverIcon ? ERROR_HIGHLIGHTED_SPRITE : ERROR_SPRITE;
                Identifier identifier = joinWithErrorSprite = isOverIcon ? MARKED_JOIN_HIGHLIGHTED_SPRITE : MARKED_JOIN_SPRITE;
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinWithErrorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    return;
                }
                if (this.summary.isLocked()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    if (isOverIcon) {
                        graphics.setTooltipForNextFrame(this.minecraft.font.split(WORLD_LOCKED_TOOLTIP, 175), mouseX, mouseY);
                    }
                } else if (this.summary.requiresManualConversion()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    if (isOverIcon) {
                        graphics.setTooltipForNextFrame(this.minecraft.font.split(WORLD_REQUIRES_CONVERSION, 175), mouseX, mouseY);
                    }
                } else if (!this.summary.isCompatible()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    if (isOverIcon) {
                        graphics.setTooltipForNextFrame(this.minecraft.font.split(INCOMPATIBLE_VERSION_TOOLTIP, 175), mouseX, mouseY);
                    }
                } else if (this.summary.shouldBackup()) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinWithErrorSprite, this.getContentX(), this.getContentY(), 32, 32);
                    if (this.summary.isDowngrade()) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, errorSprite, this.getContentX(), this.getContentY(), 32, 32);
                        if (isOverIcon) {
                            graphics.setTooltipForNextFrame((List<FormattedCharSequence>)ImmutableList.of((Object)FROM_NEWER_TOOLTIP_1.getVisualOrderText(), (Object)FROM_NEWER_TOOLTIP_2.getVisualOrderText()), mouseX, mouseY);
                        }
                    } else if (!SharedConstants.getCurrentVersion().stable()) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, warningSprite, this.getContentX(), this.getContentY(), 32, 32);
                        if (isOverIcon) {
                            graphics.setTooltipForNextFrame((List<FormattedCharSequence>)ImmutableList.of((Object)SNAPSHOT_TOOLTIP_1.getVisualOrderText(), (Object)SNAPSHOT_TOOLTIP_2.getVisualOrderText()), mouseX, mouseY);
                        }
                    }
                    if (isOverIcon) {
                        this.this$0.handleCursor(graphics);
                    }
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, joinSprite, this.getContentX(), this.getContentY(), 32, 32);
                    if (isOverIcon) {
                        this.this$0.handleCursor(graphics);
                    }
                }
            }
        }

        private int getTextX() {
            return this.getContentX() + 32 + 3;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.canInteract()) {
                int relX = (int)event.x() - this.getContentX();
                int relY = (int)event.y() - this.getContentY();
                if (doubleClick || this.mouseOverIcon(relX, relY, 32) && this.list.entryType == EntryType.SINGLEPLAYER) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                    Consumer<WorldListEntry> onEntryInteract = this.list.onEntryInteract;
                    if (onEntryInteract != null) {
                        onEntryInteract.accept(this);
                        return true;
                    }
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (event.isSelection() && this.canInteract()) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                Consumer<WorldListEntry> onEntryInteract = this.list.onEntryInteract;
                if (onEntryInteract != null) {
                    onEntryInteract.accept(this);
                    return true;
                }
            }
            return super.keyPressed(event);
        }

        public boolean canInteract() {
            return this.summary.primaryActionActive() || this.list.entryType == EntryType.UPLOAD_WORLD;
        }

        public void joinWorld() {
            if (!this.summary.primaryActionActive()) {
                return;
            }
            if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
                this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
                return;
            }
            this.minecraft.createWorldOpenFlows().openWorld(this.summary.getLevelId(), this.list::returnToScreen);
        }

        public void deleteWorld() {
            this.minecraft.gui.setScreen(new ConfirmScreen(result -> {
                if (result) {
                    this.minecraft.gui.setScreen(new ProgressScreen(true));
                    this.doDeleteWorld();
                }
                this.list.returnToScreen();
            }, Component.translatable("selectWorld.deleteQuestion"), Component.translatable("selectWorld.deleteWarning", this.summary.getLevelName()), Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
        }

        public void doDeleteWorld() {
            LevelStorageSource levelSource = this.minecraft.getLevelSource();
            String levelId = this.summary.getLevelId();
            try (LevelStorageSource.LevelStorageAccess access = levelSource.createAccess(levelId);){
                access.deleteLevel();
            }
            catch (IOException e) {
                SystemToast.onWorldDeleteFailure(this.minecraft, levelId);
                LOGGER.error("Failed to delete world {}", (Object)levelId, (Object)e);
            }
        }

        public void editWorld() {
            EditWorldScreen editScreen;
            LevelStorageSource.LevelStorageAccess access;
            this.queueLoadScreen();
            String levelId = this.summary.getLevelId();
            try {
                access = this.minecraft.getLevelSource().validateAndCreateAccess(levelId);
            }
            catch (IOException e) {
                SystemToast.onWorldAccessFailure(this.minecraft, levelId);
                LOGGER.error("Failed to access level {}", (Object)levelId, (Object)e);
                this.list.reloadWorldList();
                return;
            }
            catch (ContentValidationException e) {
                LOGGER.warn("{}", (Object)e.getMessage());
                this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
                return;
            }
            try {
                editScreen = EditWorldScreen.create(this.minecraft, access, result -> {
                    access.safeClose();
                    this.list.returnToScreen();
                });
            }
            catch (IOException | NbtException | ReportedNbtException e) {
                access.safeClose();
                SystemToast.onWorldAccessFailure(this.minecraft, levelId);
                LOGGER.error("Failed to load world data {}", (Object)levelId, (Object)e);
                this.list.reloadWorldList();
                return;
            }
            this.minecraft.gui.setScreen(editScreen);
        }

        public void recreateWorld() {
            this.queueLoadScreen();
            try (LevelStorageSource.LevelStorageAccess access = this.minecraft.getLevelSource().validateAndCreateAccess(this.summary.getLevelId());){
                Pair<LevelSettings, WorldCreationContext> recreatedSettings = this.minecraft.createWorldOpenFlows().recreateWorldData(access);
                LevelSettings levelSettings = (LevelSettings)recreatedSettings.getFirst();
                WorldCreationContext creationContext = (WorldCreationContext)recreatedSettings.getSecond();
                Path dataPackDir = CreateWorldScreen.createTempDataPackDirFromExistingWorld(access.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft);
                creationContext.validate();
                if (creationContext.options().isOldCustomizedWorld()) {
                    this.minecraft.gui.setScreen(new ConfirmScreen(result -> this.minecraft.gui.setScreen(result ? CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, creationContext, dataPackDir) : this.screen), Component.translatable("selectWorld.recreate.customized.title"), Component.translatable("selectWorld.recreate.customized.text"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
                } else {
                    this.minecraft.gui.setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.list::returnToScreen, levelSettings, creationContext, dataPackDir));
                }
            }
            catch (ContentValidationException e) {
                LOGGER.warn("{}", (Object)e.getMessage());
                this.minecraft.gui.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.gui.setScreen(this.screen)));
            }
            catch (Exception e) {
                LOGGER.error("Unable to recreate world", (Throwable)e);
                this.minecraft.gui.setScreen(new AlertScreen(() -> this.minecraft.gui.setScreen(this.screen), Component.translatable("selectWorld.recreate.error.title"), (Component)Component.translatable("selectWorld.recreate.error.text")));
            }
        }

        private void queueLoadScreen() {
            this.minecraft.setScreenAndShow(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        }

        private void loadIcon() {
            boolean shouldHaveIcon;
            boolean bl = shouldHaveIcon = this.iconFile != null && Files.isRegularFile(this.iconFile, new LinkOption[0]);
            if (shouldHaveIcon) {
                try (InputStream stream = Files.newInputStream(this.iconFile, new OpenOption[0]);){
                    this.icon.upload(NativeImage.read(stream));
                }
                catch (Throwable t) {
                    LOGGER.error("Invalid icon for world {}", (Object)this.summary.getLevelId(), (Object)t);
                    this.iconFile = null;
                }
            } else {
                this.icon.clear();
            }
        }

        @Override
        public void close() {
            if (!this.icon.isClosed()) {
                this.icon.close();
            }
        }

        public String getLevelName() {
            return this.summary.getLevelName();
        }

        @Override
        public LevelSummary getLevelSummary() {
            return this.summary;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static abstract class Entry
    extends ObjectSelectionList.Entry<Entry>
    implements AutoCloseable {
        @Override
        public void close() {
        }

        public @Nullable LevelSummary getLevelSummary() {
            return null;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final Minecraft minecraft;
        private final Screen screen;
        private int width;
        private int height;
        private String filter = "";
        private EntryType type = EntryType.SINGLEPLAYER;
        private @Nullable WorldSelectionList oldList = null;
        private @Nullable Consumer<LevelSummary> onEntrySelect = null;
        private @Nullable Consumer<WorldListEntry> onEntryInteract = null;

        public Builder(Minecraft minecraft, Screen screen) {
            this.minecraft = minecraft;
            this.screen = screen;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public Builder oldList(@Nullable WorldSelectionList oldList) {
            this.oldList = oldList;
            return this;
        }

        public Builder onEntrySelect(Consumer<LevelSummary> onEntrySelect) {
            this.onEntrySelect = onEntrySelect;
            return this;
        }

        public Builder onEntryInteract(Consumer<WorldListEntry> onEntryInteract) {
            this.onEntryInteract = onEntryInteract;
            return this;
        }

        public Builder uploadWorld() {
            this.type = EntryType.UPLOAD_WORLD;
            return this;
        }

        public WorldSelectionList build() {
            return new WorldSelectionList(this.screen, this.minecraft, this.width, this.height, this.filter, this.oldList, this.onEntrySelect, this.onEntryInteract, this.type);
        }
    }
}

