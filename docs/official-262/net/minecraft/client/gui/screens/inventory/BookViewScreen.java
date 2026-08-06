/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.inventory;

import java.lang.runtime.SwitchBootstraps;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class BookViewScreen
extends Screen {
    public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
    public static final int PAGE_TEXT_X_OFFSET = 36;
    public static final int PAGE_TEXT_Y_OFFSET = 30;
    private static final int BACKGROUND_TEXTURE_WIDTH = 256;
    private static final int BACKGROUND_TEXTURE_HEIGHT = 256;
    private static final Component TITLE = Component.translatable("book.view.title");
    private static final Style PAGE_TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(-16777216);
    public static final BookAccess EMPTY_ACCESS = new BookAccess(List.of());
    public static final Identifier BOOK_LOCATION = Identifier.withDefaultNamespace("textures/gui/book.png");
    protected static final int TEXT_WIDTH = 114;
    protected static final int TEXT_HEIGHT = 128;
    protected static final int IMAGE_WIDTH = 192;
    private static final int PAGE_INDICATOR_X_OFFSET = 148;
    protected static final int IMAGE_HEIGHT = 192;
    private static final int PAGE_BUTTON_Y = 157;
    private static final int PAGE_BACK_BUTTON_X = 43;
    private static final int PAGE_FORWARD_BUTTON_X = 116;
    private BookAccess bookAccess;
    private int currentPage;
    private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
    private int cachedPage = -1;
    private Component pageMsg = CommonComponents.EMPTY;
    private PageButton forwardButton;
    private PageButton backButton;
    private final boolean playTurnSound;

    public BookViewScreen(BookAccess bookAccess) {
        this(bookAccess, true);
    }

    public BookViewScreen() {
        this(EMPTY_ACCESS, false);
    }

    private BookViewScreen(BookAccess bookAccess, boolean playTurnSound) {
        super(TITLE);
        this.bookAccess = bookAccess;
        this.playTurnSound = playTurnSound;
    }

    public void setBookAccess(BookAccess bookAccess) {
        this.bookAccess = bookAccess;
        this.currentPage = Mth.clamp(this.currentPage, 0, bookAccess.getPageCount());
        this.updateButtonVisibility();
        this.cachedPage = -1;
    }

    public boolean setPage(int page) {
        int clampedPage = Mth.clamp(page, 0, this.bookAccess.getPageCount() - 1);
        if (clampedPage != this.currentPage) {
            this.currentPage = clampedPage;
            this.updateButtonVisibility();
            this.cachedPage = -1;
            return true;
        }
        return false;
    }

    protected boolean forcePage(int page) {
        return this.setPage(page);
    }

    @Override
    protected void init() {
        this.createMenuControls();
        this.createPageControlButtons();
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinLines(super.getNarrationMessage(), this.getPageNumberMessage(), this.bookAccess.getPage(this.currentPage));
    }

    private Component getPageNumberMessage() {
        return Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1)).withStyle(PAGE_TEXT_STYLE);
    }

    protected void createMenuControls() {
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).pos((this.width - 200) / 2, this.menuControlsTop()).width(200).build());
    }

    protected void createPageControlButtons() {
        int left = this.backgroundLeft();
        int top = this.backgroundTop();
        this.forwardButton = this.addRenderableWidget(new PageButton(left + 116, top + 157, true, button -> this.pageForward(), this.playTurnSound));
        this.backButton = this.addRenderableWidget(new PageButton(left + 43, top + 157, false, button -> this.pageBack(), this.playTurnSound));
        this.updateButtonVisibility();
    }

    private int getNumPages() {
        return this.bookAccess.getPageCount();
    }

    protected void pageBack() {
        if (this.currentPage > 0) {
            --this.currentPage;
        }
        this.updateButtonVisibility();
    }

    protected void pageForward() {
        if (this.currentPage < this.getNumPages() - 1) {
            ++this.currentPage;
        }
        this.updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
        this.backButton.visible = this.currentPage > 0;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        return switch (event.key()) {
            case 266 -> {
                this.backButton.onPress(event);
                yield true;
            }
            case 267 -> {
                this.forwardButton.onPress(event);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.visitText(graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR), false);
    }

    private void visitText(ActiveTextCollector collector, boolean clickableOnly) {
        if (this.cachedPage != this.currentPage) {
            Component pageText = ComponentUtils.mergeStyles(this.bookAccess.getPage(this.currentPage), PAGE_TEXT_STYLE);
            this.cachedPageComponents = this.font.split(pageText, 114);
            this.pageMsg = this.getPageNumberMessage();
            this.cachedPage = this.currentPage;
        }
        int left = this.backgroundLeft();
        int top = this.backgroundTop();
        if (!clickableOnly) {
            collector.accept(TextAlignment.RIGHT, left + 148, top + 16, this.pageMsg);
        }
        int shownLines = Math.min(128 / this.font.lineHeight, this.cachedPageComponents.size());
        for (int i = 0; i < shownLines; ++i) {
            FormattedCharSequence component = this.cachedPageComponents.get(i);
            collector.accept(left + 36, top + 30 + i * this.font.lineHeight, component);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_LOCATION, this.backgroundLeft(), this.backgroundTop(), 0.0f, 0.0f, 192, 192, 256, 256);
    }

    private int backgroundLeft() {
        return (this.width - 192) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    protected int menuControlsTop() {
        return this.backgroundTop() + 192 + 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(this.font, (int)event.x(), (int)event.y());
            this.visitText(finder, true);
            Style clickedStyle = finder.result();
            if (clickedStyle != null && this.handleClickEvent(clickedStyle.getClickEvent())) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    protected boolean handleClickEvent(@Nullable ClickEvent event) {
        int n2;
        if (event == null) {
            return false;
        }
        LocalPlayer player = Objects.requireNonNull(this.minecraft.player, "Player not available");
        ClickEvent clickEvent = event;
        Objects.requireNonNull(clickEvent);
        ClickEvent clickEvent2 = clickEvent;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{ClickEvent.ChangePage.class, ClickEvent.RunCommand.class}, (ClickEvent)clickEvent2, n)) {
            case 0: {
                ClickEvent.ChangePage changePage = (ClickEvent.ChangePage)clickEvent2;
                try {
                    int n3 = n2 = changePage.page();
                }
                catch (Throwable throwable) {
                    throw new MatchException(throwable.toString(), throwable);
                }
            }
            int page = n2;
            this.forcePage(page - 1);
            return true;
            case 1: {
                String command;
                ClickEvent.RunCommand runCommand = (ClickEvent.RunCommand)clickEvent2;
                {
                    String string;
                    command = string = runCommand.command();
                    this.closeContainerOnServer();
                }
                BookViewScreen.clickCommandAction(player, command, null);
                return true;
            }
        }
        BookViewScreen.defaultHandleGameClickEvent(event, this.minecraft, this);
        return true;
    }

    protected void closeContainerOnServer() {
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Environment(value=EnvType.CLIENT)
    public record BookAccess(List<Component> pages) {
        public int getPageCount() {
            return this.pages.size();
        }

        public Component getPage(int page) {
            if (page >= 0 && page < this.getPageCount()) {
                return this.pages.get(page);
            }
            return CommonComponents.EMPTY;
        }

        public static @Nullable BookAccess fromItem(ItemStack itemStack) {
            boolean filterEnabled = Minecraft.getInstance().isTextFilteringEnabled();
            WrittenBookContent writtenContent = itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (writtenContent != null) {
                return new BookAccess(writtenContent.getPages(filterEnabled));
            }
            WritableBookContent writableContent = itemStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (writableContent != null) {
                return new BookAccess(writableContent.getPages(filterEnabled).map(Component::literal).toList());
            }
            return null;
        }
    }
}

