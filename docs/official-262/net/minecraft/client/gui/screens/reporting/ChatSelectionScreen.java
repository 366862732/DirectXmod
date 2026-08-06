/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.minecraft.report.AbuseReportLimits
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatSelectionLogFiller;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class ChatSelectionScreen
extends Screen {
    private static final Identifier CHECKMARK_SPRITE = Identifier.withDefaultNamespace("icon/checkmark");
    private static final Component TITLE = Component.translatable("gui.chatSelection.title");
    private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context");
    private final @Nullable Screen lastScreen;
    private final ReportingContext reportingContext;
    private Button confirmSelectedButton;
    private MultiLineLabel contextInfoLabel;
    private @Nullable ChatSelectionList chatSelectionList;
    private final ChatReport.Builder report;
    private final Consumer<ChatReport.Builder> onSelected;
    private ChatSelectionLogFiller chatLogFiller;

    public ChatSelectionScreen(@Nullable Screen lastScreen, ReportingContext reportingContext, ChatReport.Builder report, Consumer<ChatReport.Builder> onSelected) {
        super(TITLE);
        this.lastScreen = lastScreen;
        this.reportingContext = reportingContext;
        this.report = report.copy();
        this.onSelected = onSelected;
    }

    @Override
    protected void init() {
        this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
        this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
        this.chatSelectionList = this.addRenderableWidget(new ChatSelectionList(this, this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * this.font.lineHeight));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> this.onClose()).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build());
        this.confirmSelectedButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            this.onSelected.accept(this.report);
            this.onClose();
        }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
        this.updateConfirmSelectedButton();
        this.extendLog();
        this.chatSelectionList.setScrollAmount(this.chatSelectionList.maxScrollAmount());
    }

    private boolean canReport(LoggedChatMessage message) {
        return message.canReport(this.report.reportedProfileId());
    }

    private void extendLog() {
        int pageSize = this.chatSelectionList.getMaxVisibleEntries();
        this.chatLogFiller.fillNextPage(pageSize, this.chatSelectionList);
    }

    private void onReachedScrollTop() {
        this.extendLog();
    }

    private void updateConfirmSelectedButton() {
        this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        ActiveTextCollector textRenderer = graphics.textRenderer();
        graphics.centeredText(this.font, this.title, this.width / 2, 10, -1);
        AbuseReportLimits reportLimits = this.reportingContext.sender().reportLimits();
        int messageCount = this.report.reportedMessages().size();
        int maxMessageCount = reportLimits.maxReportedMessageCount();
        MutableComponent selectedText = Component.translatable("gui.chatSelection.selected", messageCount, maxMessageCount);
        graphics.centeredText(this.font, selectedText, this.width / 2, 26, -1);
        int topY = this.chatSelectionList.getFooterTop();
        this.contextInfoLabel.visitLines(TextAlignment.CENTER, this.width / 2, topY, this.font.lineHeight, textRenderer);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
    }

    @Environment(value=EnvType.CLIENT)
    public class ChatSelectionList
    extends ObjectSelectionList<Entry>
    implements ChatSelectionLogFiller.Output {
        public static final int ITEM_HEIGHT = 16;
        private @Nullable Heading previousHeading;
        final /* synthetic */ ChatSelectionScreen this$0;

        public ChatSelectionList(ChatSelectionScreen this$0, Minecraft minecraft, int upperMargin) {
            ChatSelectionScreen chatSelectionScreen = this$0;
            Objects.requireNonNull(chatSelectionScreen);
            this.this$0 = chatSelectionScreen;
            super(minecraft, this$0.width, this$0.height - upperMargin - 80, 40, 16);
        }

        @Override
        public void setScrollAmount(double scrollAmount) {
            double prevScrollAmount = this.scrollAmount();
            super.setScrollAmount(scrollAmount);
            if ((float)this.maxScrollAmount() > 1.0E-5f && scrollAmount <= (double)1.0E-5f && !Mth.equal(scrollAmount, prevScrollAmount)) {
                this.this$0.onReachedScrollTop();
            }
        }

        @Override
        public void acceptMessage(int id, LoggedChatMessage.Player message) {
            boolean canReport = message.canReport(this.this$0.report.reportedProfileId());
            ChatTrustLevel trustLevel = message.trustLevel();
            GuiMessageTag tag = trustLevel.createTag(message.message());
            MessageEntry entry = new MessageEntry(this, id, message.toContentComponent(), message.toNarrationComponent(), tag, canReport, true);
            this.addEntryToTop(entry);
            this.updateHeading(message, canReport);
        }

        private void updateHeading(LoggedChatMessage.Player message, boolean canReport) {
            MessageHeadingEntry entry = new MessageHeadingEntry(this, message.profile(), message.toHeadingComponent(), canReport);
            this.addEntryToTop(entry);
            Heading heading = new Heading(message.profileId(), entry);
            if (this.previousHeading != null && this.previousHeading.canCombine(heading)) {
                this.removeEntryFromTop(this.previousHeading.entry());
            }
            this.previousHeading = heading;
        }

        @Override
        public void acceptDivider(Component text) {
            this.addEntryToTop(new PaddingEntry());
            this.addEntryToTop(new DividerEntry(this, text));
            this.addEntryToTop(new PaddingEntry());
            this.previousHeading = null;
        }

        @Override
        public int getRowWidth() {
            return Math.min(350, this.width - 50);
        }

        public int getMaxVisibleEntries() {
            return Mth.positiveCeilDiv(this.height, 16);
        }

        @Override
        protected void extractItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, Entry entry) {
            if (this.shouldHighlightEntry(entry)) {
                boolean selected = this.getSelected() == entry;
                int outlineColor = this.isFocused() && selected ? -1 : -8355712;
                this.extractSelection(graphics, entry, outlineColor);
            }
            entry.extractContent(graphics, mouseX, mouseY, this.getHovered() == entry, a);
        }

        private boolean shouldHighlightEntry(Entry entry) {
            if (entry.canSelect()) {
                boolean entrySelected = this.getSelected() == entry;
                boolean nothingSelected = this.getSelected() == null;
                boolean entryHovered = this.getHovered() == entry;
                return entrySelected || nothingSelected && entryHovered && entry.canReport();
            }
            return false;
        }

        @Override
        protected @Nullable Entry nextEntry(ScreenDirection dir) {
            return this.nextEntry(dir, Entry::canSelect);
        }

        @Override
        public void setSelected(@Nullable Entry selected) {
            super.setSelected(selected);
            Entry entry = this.nextEntry(ScreenDirection.UP);
            if (entry == null) {
                this.this$0.onReachedScrollTop();
            }
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            Entry selected = (Entry)this.getSelected();
            if (selected != null && selected.keyPressed(event)) {
                return true;
            }
            return super.keyPressed(event);
        }

        public int getFooterTop() {
            return this.getBottom() + ((ChatSelectionScreen)this.this$0).font.lineHeight;
        }

        @Environment(value=EnvType.CLIENT)
        public class MessageEntry
        extends Entry {
            private static final int CHECKMARK_WIDTH = 9;
            private static final int CHECKMARK_HEIGHT = 8;
            private static final int INDENT_AMOUNT = 11;
            private static final int TAG_MARGIN_LEFT = 4;
            private final int chatId;
            private final FormattedText text;
            private final Component narration;
            private final @Nullable List<FormattedCharSequence> hoverText;
            private final @Nullable GuiMessageTag.Icon tagIcon;
            private final @Nullable List<FormattedCharSequence> tagHoverText;
            private final boolean canReport;
            private final boolean playerMessage;
            final /* synthetic */ ChatSelectionList this$1;

            public MessageEntry(ChatSelectionList this$1, int chatId, Component text, @Nullable Component narration, GuiMessageTag tag, boolean canReport, boolean playerMessage) {
                ChatSelectionList chatSelectionList = this$1;
                Objects.requireNonNull(chatSelectionList);
                this.this$1 = chatSelectionList;
                this.chatId = chatId;
                this.tagIcon = Optionull.map(tag, GuiMessageTag::icon);
                this.tagHoverText = tag != null && tag.text() != null ? this$1.this$0.font.split(tag.text(), this$1.getRowWidth()) : null;
                this.canReport = canReport;
                this.playerMessage = playerMessage;
                FormattedText shortText = this$1.this$0.font.substrByWidth(text, this.getMaximumTextWidth() - this$1.this$0.font.width(CommonComponents.ELLIPSIS));
                if (text != shortText) {
                    this.text = FormattedText.composite(shortText, CommonComponents.ELLIPSIS);
                    this.hoverText = this$1.this$0.font.split(text, this$1.getRowWidth());
                } else {
                    this.text = text;
                    this.hoverText = null;
                }
                this.narration = narration;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                if (this.isSelected() && this.canReport) {
                    this.extractSelectedCheckmark(graphics, this.getContentY(), this.getContentX(), this.getContentHeight());
                }
                int textX = this.getContentX() + this.getTextIndent();
                int textY = this.getContentY() + 1 + (this.getContentHeight() - ((ChatSelectionScreen)this.this$1.this$0).font.lineHeight) / 2;
                graphics.text(this.this$1.this$0.font, Language.getInstance().getVisualOrder(this.text), textX, textY, this.canReport ? -1 : -1593835521);
                if (this.hoverText != null && hovered) {
                    graphics.setTooltipForNextFrame(this.hoverText, mouseX, mouseY);
                }
                int textWidth = this.this$1.this$0.font.width(this.text);
                this.extractTag(graphics, textX + textWidth + 4, this.getContentY(), this.getContentHeight(), mouseX, mouseY);
            }

            private void extractTag(GuiGraphicsExtractor graphics, int iconLeft, int rowTop, int rowHeight, int mouseX, int mouseY) {
                if (this.tagIcon != null) {
                    int iconTop = rowTop + (rowHeight - this.tagIcon.height) / 2;
                    this.tagIcon.extractRenderState(graphics, iconLeft, iconTop);
                    if (this.tagHoverText != null && mouseX >= iconLeft && mouseX <= iconLeft + this.tagIcon.width && mouseY >= iconTop && mouseY <= iconTop + this.tagIcon.height) {
                        graphics.setTooltipForNextFrame(this.tagHoverText, mouseX, mouseY);
                    }
                }
            }

            private void extractSelectedCheckmark(GuiGraphicsExtractor graphics, int rowTop, int rowLeft, int rowHeight) {
                int left = rowLeft;
                int top = rowTop + (rowHeight - 8) / 2;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CHECKMARK_SPRITE, left, top, 9, 8);
            }

            private int getMaximumTextWidth() {
                int tagMargin = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
                return this.this$1.getRowWidth() - this.getTextIndent() - 4 - tagMargin;
            }

            private int getTextIndent() {
                return this.playerMessage ? 11 : 0;
            }

            @Override
            public Component getNarration() {
                return this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                this.this$1.setSelected((Entry)null);
                return this.toggleReport();
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    return this.toggleReport();
                }
                return false;
            }

            @Override
            public boolean isSelected() {
                return this.this$1.this$0.report.isReported(this.chatId);
            }

            @Override
            public boolean canSelect() {
                return true;
            }

            @Override
            public boolean canReport() {
                return this.canReport;
            }

            private boolean toggleReport() {
                if (this.canReport) {
                    this.this$1.this$0.report.toggleReported(this.chatId);
                    this.this$1.this$0.updateConfirmSelectedButton();
                    return true;
                }
                return false;
            }
        }

        @Environment(value=EnvType.CLIENT)
        public class MessageHeadingEntry
        extends Entry {
            private static final int FACE_SIZE = 12;
            private static final int PADDING = 4;
            private final Component heading;
            private final Supplier<PlayerSkin> skin;
            private final boolean canReport;
            final /* synthetic */ ChatSelectionList this$1;

            public MessageHeadingEntry(ChatSelectionList this$1, GameProfile profile, Component heading, boolean canReport) {
                ChatSelectionList chatSelectionList = this$1;
                Objects.requireNonNull(chatSelectionList);
                this.this$1 = chatSelectionList;
                this.heading = heading;
                this.canReport = canReport;
                this.skin = this$1.minecraft.getSkinManager().createLookup(profile, true);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int faceX = this.getContentX() - 12 + 4;
                int faceY = this.getContentY() + (this.getContentHeight() - 12) / 2;
                PlayerFaceExtractor.extractRenderState(graphics, this.skin.get(), faceX, faceY, 12);
                int textY = this.getContentY() + 1 + (this.getContentHeight() - ((ChatSelectionScreen)this.this$1.this$0).font.lineHeight) / 2;
                graphics.text(this.this$1.this$0.font, this.heading, faceX + 12 + 4, textY, this.canReport ? -1 : -1593835521);
            }
        }

        @Environment(value=EnvType.CLIENT)
        private record Heading(UUID sender, Entry entry) {
            public boolean canCombine(Heading other) {
                return other.sender.equals(this.sender);
            }
        }

        @Environment(value=EnvType.CLIENT)
        public static abstract class Entry
        extends ObjectSelectionList.Entry<Entry> {
            @Override
            public Component getNarration() {
                return CommonComponents.EMPTY;
            }

            public boolean isSelected() {
                return false;
            }

            public boolean canSelect() {
                return false;
            }

            public boolean canReport() {
                return this.canSelect();
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                return this.canSelect();
            }
        }

        @Environment(value=EnvType.CLIENT)
        public static class PaddingEntry
        extends Entry {
            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            }
        }

        @Environment(value=EnvType.CLIENT)
        public class DividerEntry
        extends Entry {
            private final Component text;
            final /* synthetic */ ChatSelectionList this$1;

            public DividerEntry(ChatSelectionList this$1, Component text) {
                ChatSelectionList chatSelectionList = this$1;
                Objects.requireNonNull(chatSelectionList);
                this.this$1 = chatSelectionList;
                this.text = text;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                int centerY = this.getContentYMiddle();
                int rowRight = this.getContentRight() - 8;
                int textWidth = this.this$1.this$0.font.width(this.text);
                int textLeft = (this.getContentX() + rowRight - textWidth) / 2;
                int textTop = centerY - ((ChatSelectionScreen)this.this$1.this$0).font.lineHeight / 2;
                graphics.text(this.this$1.this$0.font, this.text, textLeft, textTop, -6250336);
            }

            @Override
            public Component getNarration() {
                return this.text;
            }
        }
    }
}

