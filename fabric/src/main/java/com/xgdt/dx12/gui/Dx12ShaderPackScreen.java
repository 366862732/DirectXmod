package com.xgdt.dx12.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xgdt.dx12.dx12.Dx12ShaderPackRenderer;
import com.xgdt.dx12.shaderpack.ShaderPack;
import com.xgdt.dx12.shaderpack.ShaderPackManager;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * 光影包设置界面（布局照搬 Iris）：左侧光影包列表，右侧选中包的设置项，
 * 底部启用开关 / 重载 / 打开文件夹 / 完成。
 *
 * <p>光影包是 {@code .zip}（也支持已解压目录），放在 {@code <game_dir>/shaderpacks/}。
 */
public class Dx12ShaderPackScreen extends Screen {
    private static final int TOP_BAR_HEIGHT = 28;
    private static final int PACK_ROW_HEIGHT = 32;
    private static final int SETTING_ROW_HEIGHT = 21;
    private static final int PADDING = 6;
    private static final int SLIDER_WIDTH = 90;
    private static final int SLIDER_HEIGHT = 10;
    private static final int SLIDER_THUMB_WIDTH = 4;
    private static final int VALUE_GAP = 6;
    private static final int CHECKBOX_SIZE = 10;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A4A8;
    private static final int ACCENT = 0xFF4FC3F7;
    private static final int ROW_BG = 0x40000000;
    private static final int ROW_BG_HOVER = 0xE0000000;
    private static final int FIELD_BORDER = 0x30FFFFFF;

    private final Screen parent;
    private final ShaderPackManager manager = ShaderPackManager.getInstance();

    private List<ShaderPackManager.PackEntry> packs = List.of();
    private List<ShaderPack.Setting> settings = List.of();

    private int packScroll;
    private int packMaxScroll;
    private int settingScroll;
    private int settingMaxScroll;

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int settingsX;
    private int settingsY;
    private int settingsW;
    private int settingsH;

    private int closeX;
    private int closeY;

    private int enabledX;
    private int enabledY;
    private int enabledW;
    private int enabledH;
    private int reloadX;
    private int reloadY;
    private int reloadW;
    private int reloadH;
    private int folderX;
    private int folderY;
    private int folderW;
    private int folderH;
    private int doneX;
    private int doneY;
    private int doneW;
    private int doneH;

    private int draggingSetting = -1;

    public Dx12ShaderPackScreen(Screen parent) {
        super(Component.literal("Shader Packs"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        try {
            Files.createDirectories(this.manager.directory());
        } catch (Exception ignored) {
            // 目录创建失败时列表为空，界面仍可打开。
        }
        this.refresh();
    }

    private void refresh() {
        this.packs = this.manager.list();
        this.settings = this.manager.currentSettings();
        this.packScroll = 0;
        this.settingScroll = 0;
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                  float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float a) {
        this.layout();

        graphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, 0x90000000);
        graphics.fill(0, TOP_BAR_HEIGHT - 1, this.width, TOP_BAR_HEIGHT, 0x30FFFFFF);

        boolean chinese = this.isChinese();
        graphics.text(this.font, chinese ? "光影包" : "Shader Packs", 10, 10, TEXT);
        String backend = RenderSystem.getDevice().getDeviceInfo().backendName();
        Component backendLabel = Component.literal("DX12".equals(backend) ? "DirectX 12" : backend);
        graphics.text(this.font, backendLabel,
            this.closeX - 10 - this.font.width(backendLabel), 10, ACCENT);

        int color = this.isInside(mouseX, mouseY, this.closeX - 3, this.closeY - 2, 14, 14)
            ? TEXT : TEXT_DIM;
        for (int i = 0; i < 8; i++) {
            graphics.fill(this.closeX + i, this.closeY + i, this.closeX + i + 1,
                this.closeY + i + 1, color);
            graphics.fill(this.closeX + 7 - i, this.closeY + i, this.closeX + 8 - i,
                this.closeY + i + 1, color);
        }

        this.drawPacks(graphics, mouseX, mouseY, chinese);
        this.drawSettings(graphics, mouseX, mouseY, chinese);
        this.drawFooter(graphics, mouseX, mouseY, chinese);
    }

    private void layout() {
        this.closeX = this.width - 22;
        this.closeY = 7;

        int footerHeight = 26;
        int contentTop = TOP_BAR_HEIGHT + 6;
        int contentHeight = Math.max(40, this.height - contentTop - footerHeight);
        this.listX = 8;
        this.listY = contentTop;
        this.listW = Mth.clamp(this.width / 4, 130, 220);
        this.listH = contentHeight;

        this.settingsX = this.listX + this.listW + 8;
        this.settingsY = contentTop;
        this.settingsW = Math.max(120, this.width - this.settingsX - 8);
        this.settingsH = contentHeight;

        int buttonH = 18;
        int footerY = this.height - 6 - buttonH;
        this.doneW = this.font.width(CommonComponents.GUI_DONE) + 18;
        this.doneX = this.width - 8 - this.doneW;
        this.doneY = footerY;
        this.doneH = buttonH;
        this.folderW = this.font.width(this.tr("打开文件夹", "Open Folder")) + 18;
        this.folderX = this.doneX - 6 - this.folderW;
        this.folderY = footerY;
        this.folderH = buttonH;
        this.reloadW = this.font.width(this.tr("重新加载", "Reload")) + 18;
        this.reloadX = this.folderX - 6 - this.reloadW;
        this.reloadY = footerY;
        this.reloadH = buttonH;

        this.enabledW = this.font.width(this.tr("启用光影：开", "Shaders: ON")) + 22;
        this.enabledX = 8;
        this.enabledY = footerY;
        this.enabledH = buttonH;

        this.packMaxScroll = Math.max(0,
            (this.packs.size() + 1) * PACK_ROW_HEIGHT - this.listH + 8);
        this.packScroll = Mth.clamp(this.packScroll, 0, this.packMaxScroll);

        int settingsTop = this.settingsTop();
        this.settingMaxScroll = Math.max(0,
            this.settings.size() * SETTING_ROW_HEIGHT
                - Math.max(0, this.settingsY + this.settingsH - settingsTop));
        this.settingScroll = Mth.clamp(this.settingScroll, 0, this.settingMaxScroll);
    }

    /** 右侧设置列表的起点（标题 / 作者 / 描述之下）。 */
    private int settingsTop() {
        return Math.min(this.settingsY + this.headerHeight(), this.settingsY + this.settingsH);
    }

    private ShaderPack activePack() {
        return this.manager.activePack();
    }

    private int headerHeight() {
        ShaderPack pack = this.activePack();
        if (pack == null) {
            return 26;
        }
        int height = 22; // 名称 + 作者/版本
        height += this.wrap(pack.description(), this.settingsW - PADDING * 2).size()
            * (this.font.lineHeight + 1);
        return height + 10;
    }

    private void drawPacks(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                           boolean chinese) {
        graphics.fill(this.listX, this.listY, this.listX + this.listW,
            this.listY + this.listH, 0x28000000);

        graphics.enableScissor(this.listX, this.listY, this.listX + this.listW,
            this.listY + this.listH);
        int y = this.listY + 4 - this.packScroll;

        // 第 0 行：关闭光影
        boolean offSelected = this.manager.activeId().isEmpty();
        boolean offHover = this.isInside(mouseX, mouseY, this.listX + 4, y, this.listW - 8,
            PACK_ROW_HEIGHT) && this.isInside(mouseX, mouseY, this.listX + 4, this.listY,
            this.listW - 8, this.listH);
        if (offHover || offSelected) {
            graphics.fill(this.listX + 4, y, this.listX + this.listW - 4,
                y + PACK_ROW_HEIGHT, offHover ? ROW_BG_HOVER : 0x33FFFFFF);
        }
        graphics.text(this.font, chinese ? "关闭（原版画面）" : "Off (Vanilla)",
            this.listX + 12, y + (PACK_ROW_HEIGHT - this.font.lineHeight) / 2,
            offSelected ? TEXT : TEXT_DIM);
        y += PACK_ROW_HEIGHT;

        for (ShaderPackManager.PackEntry entry : this.packs) {
            boolean selected = entry.id().equalsIgnoreCase(this.manager.activeId());
            boolean hover = this.isInside(mouseX, mouseY, this.listX + 4, y, this.listW - 8,
                PACK_ROW_HEIGHT) && this.isInside(mouseX, mouseY, this.listX + 4, this.listY,
                this.listW - 8, this.listH);
            if (hover || selected) {
                graphics.fill(this.listX + 4, y, this.listX + this.listW - 4,
                    y + PACK_ROW_HEIGHT, hover ? ROW_BG_HOVER : 0x33FFFFFF);
                graphics.fill(this.listX + 4, y, this.listX + 6, y + PACK_ROW_HEIGHT, ACCENT);
            }
            String name = entry.name();
            graphics.text(this.font, name, this.listX + 12, y + 5,
                entry.valid() ? TEXT : 0xFFE05C5C);
            String detail = entry.valid()
                ? (entry.passCount() + (chinese ? " 个 pass" : " passes"))
                : this.tr("无效", "Invalid");
            graphics.text(this.font, detail, this.listX + 12, y + 16, TEXT_DIM);
            y += PACK_ROW_HEIGHT;
        }
        graphics.disableScissor();

        if (this.packs.isEmpty() && this.listY + PACK_ROW_HEIGHT + 4 < this.listY + this.listH) {
            graphics.text(this.font,
                this.tr("把光影 .zip 放进 shaderpacks/", "Put .zip packs in shaderpacks/"),
                this.listX + 12, this.listY + PACK_ROW_HEIGHT + 10, TEXT_DIM);
        }

        this.drawScrollBar(graphics, this.listX + this.listW - 3, this.listY, this.listH,
            this.packScroll, this.packMaxScroll);
    }

    private void drawSettings(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                              boolean chinese) {
        ShaderPack pack = this.activePack();
        int centerX = this.settingsX + this.settingsW / 2;
        graphics.centeredText(this.font,
            Component.literal(pack == null
                ? this.tr("未启用光影", "Shader packs disabled")
                : pack.displayName()),
            centerX, this.settingsY + 4, TEXT);

        if (pack == null) {
            String hint = this.tr("在左侧选择一个光影包。", "Select a shader pack on the left.");
            graphics.centeredText(this.font, Component.literal(hint), centerX,
                this.settingsY + 24, TEXT_DIM);
            String error = this.manager.lastError();
            if (error != null && !error.isEmpty()) {
                int y = this.settingsY + 40;
                for (String line : this.wrap(error, this.settingsW - PADDING * 2)) {
                    graphics.centeredText(this.font, Component.literal(line), centerX, y,
                        0xFFE05C5C);
                    y += this.font.lineHeight + 1;
                }
            }
            return;
        }

        int y = this.settingsY + 18;
        String meta = pack.author().isEmpty() ? pack.version()
            : pack.author() + (pack.version().isEmpty() ? "" : "  ·  " + pack.version());
        if (!meta.isEmpty()) {
            graphics.centeredText(this.font, Component.literal(meta), centerX, y, TEXT_DIM);
            y += this.font.lineHeight + 2;
        }
        for (String line : this.wrap(pack.description(), this.settingsW - PADDING * 2)) {
            graphics.centeredText(this.font, Component.literal(line), centerX, y, TEXT_DIM);
            y += this.font.lineHeight + 1;
        }

        int listTop = this.settingsTop();
        int listBottom = this.settingsY + this.settingsH;
        int rowX = this.settingsX;
        int rowRight = this.settingsX + this.settingsW - 8;

        if (this.settings.isEmpty()) {
            graphics.centeredText(this.font,
                Component.literal(this.tr("该光影包没有可调选项",
                    "This shader pack has no options")),
                centerX, listTop + 6, TEXT_DIM);
            return;
        }

        graphics.enableScissor(this.settingsX, listTop, this.settingsX + this.settingsW,
            listBottom);
        for (int i = 0; i < this.settings.size(); i++) {
            int rowY = listTop + i * SETTING_ROW_HEIGHT - this.settingScroll;
            if (rowY + SETTING_ROW_HEIGHT <= listTop || rowY >= listBottom) {
                continue;
            }
            boolean hover = this.isInside(mouseX, mouseY, rowX, Math.max(rowY, listTop),
                this.settingsW - 8, SETTING_ROW_HEIGHT) && mouseY >= listTop
                && mouseY < listBottom;
            this.drawSettingRow(graphics, this.settings.get(i), i, rowX, rowY, rowRight, hover);
        }
        graphics.disableScissor();

        this.drawScrollBar(graphics, this.settingsX + this.settingsW - 3, listTop,
            listBottom - listTop, this.settingScroll, this.settingMaxScroll);
    }

    private void drawSettingRow(GuiGraphicsExtractor graphics, ShaderPack.Setting setting,
                                int index, int x, int y, int rowRight, boolean hovered) {
        graphics.fill(x, y, rowRight, y + SETTING_ROW_HEIGHT, hovered ? ROW_BG_HOVER : ROW_BG);
        int textY = y + (SETTING_ROW_HEIGHT - this.font.lineHeight) / 2;
        graphics.text(this.font, setting.name(), x + PADDING, textY, TEXT);

        int controlRight = rowRight - PADDING;
        if (setting.isBoolean()) {
            int boxX = controlRight - CHECKBOX_SIZE;
            int boxY = y + (SETTING_ROW_HEIGHT - CHECKBOX_SIZE) / 2;
            int color = setting.numericValue() != 0.0f ? ACCENT : 0xFF6A7078;
            graphics.fill(boxX, boxY, boxX + CHECKBOX_SIZE, boxY + CHECKBOX_SIZE, color);
            graphics.fill(boxX + 2, boxY + 2, boxX + CHECKBOX_SIZE - 2,
                boxY + CHECKBOX_SIZE - 2, 0xFF14171A);
            if (setting.numericValue() != 0.0f) {
                graphics.fill(boxX + 3, boxY + 3, boxX + CHECKBOX_SIZE - 3,
                    boxY + CHECKBOX_SIZE - 3, color);
            }
            return;
        }

        String value = formatNumber(setting.numericValue());
        boolean showSlider = hovered || this.draggingSetting == index;
        int valueW = this.font.width(value);
        if (showSlider) {
            int valueX = controlRight - valueW;
            graphics.text(this.font, value, valueX, textY, TEXT);
            int sliderX = valueX - VALUE_GAP - SLIDER_WIDTH;
            int trackY = y + SETTING_ROW_HEIGHT / 2;
            graphics.fill(sliderX, trackY, sliderX + SLIDER_WIDTH, trackY + 1, 0x66FFFFFF);
            int thumbX = sliderX
                + (int) (this.fraction(setting) * SLIDER_WIDTH) - SLIDER_THUMB_WIDTH / 2;
            graphics.fill(thumbX, trackY - SLIDER_HEIGHT / 2, thumbX + SLIDER_THUMB_WIDTH,
                trackY + SLIDER_HEIGHT / 2, TEXT);
        } else {
            graphics.text(this.font, value, controlRight - valueW, textY, TEXT_DIM);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                            boolean chinese) {
        boolean enabled = this.manager.isEnabled();
        String label = chinese
            ? (enabled ? "启用光影：开" : "启用光影：关")
            : (enabled ? "Shaders: ON" : "Shaders: OFF");
        this.drawButton(graphics, label, this.enabledX, this.enabledY, this.enabledW,
            this.enabledH, this.isInside(mouseX, mouseY, this.enabledX, this.enabledY,
                this.enabledW, this.enabledH), enabled ? ACCENT : TEXT_DIM);
        this.drawButton(graphics, this.tr("重新加载", "Reload"), this.reloadX, this.reloadY,
            this.reloadW, this.reloadH, this.isInside(mouseX, mouseY, this.reloadX,
                this.reloadY, this.reloadW, this.reloadH), TEXT);
        this.drawButton(graphics, this.tr("打开文件夹", "Open Folder"), this.folderX,
            this.folderY, this.folderW, this.folderH, this.isInside(mouseX, mouseY,
                this.folderX, this.folderY, this.folderW, this.folderH), TEXT);
        this.drawButton(graphics, CommonComponents.GUI_DONE.getString(), this.doneX, this.doneY,
            this.doneW, this.doneH, this.isInside(mouseX, mouseY, this.doneX, this.doneY,
                this.doneW, this.doneH), TEXT);
    }

    private void drawButton(GuiGraphicsExtractor graphics, String label, int x, int y, int w,
                            int h, boolean hovered, int textColor) {
        graphics.fill(x, y, x + w, y + h, hovered ? 0xE0000000 : 0x60000000);
        graphics.outline(x, y, w, h, hovered ? 0x60FFFFFF : FIELD_BORDER);
        graphics.centeredText(this.font, label, x + w / 2,
            y + (h - this.font.lineHeight) / 2, textColor);
    }

    private void drawScrollBar(GuiGraphicsExtractor graphics, int barX, int barTop,
                               int barHeight, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        graphics.fill(barX, barTop, barX + 2, barTop + barHeight, 0x20FFFFFF);
        int thumbHeight = Math.max(12,
            barHeight * barHeight / (barHeight + maxScroll));
        int thumbY = barTop + (barHeight - thumbHeight) * scroll / maxScroll;
        graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0x80FFFFFF);
    }

    // ------------------------------------------------------------------
    // 交互
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (this.isInside(mx, my, this.closeX - 3, this.closeY - 2, 14, 14)) {
            this.onClose();
            return true;
        }
        if (this.isInside(mx, my, this.doneX, this.doneY, this.doneW, this.doneH)) {
            this.onClose();
            return true;
        }
        if (this.isInside(mx, my, this.enabledX, this.enabledY, this.enabledW, this.enabledH)) {
            boolean enabled = !this.manager.isEnabled();
            if (enabled && this.manager.activeId().isEmpty() && !this.packs.isEmpty()) {
                ShaderPackManager.PackEntry first = this.packs.get(0);
                if (first.valid()) {
                    this.manager.select(first.id());
                }
            }
            this.manager.setEnabled(enabled);
            Dx12ShaderPackRenderer.getInstance().invalidate();
            this.settings = this.manager.currentSettings();
            this.playClick();
            return true;
        }
        if (this.isInside(mx, my, this.reloadX, this.reloadY, this.reloadW, this.reloadH)) {
            this.manager.reload();
            Dx12ShaderPackRenderer.getInstance().invalidate();
            this.refresh();
            this.playClick();
            return true;
        }
        if (this.isInside(mx, my, this.folderX, this.folderY, this.folderW, this.folderH)) {
            try {
                Files.createDirectories(this.manager.directory());
            } catch (Exception ignored) {
            }
            Util.getPlatform().openPath(this.manager.directory());
            return true;
        }

        // 左侧光影包列表
        if (this.isInside(mx, my, this.listX, this.listY, this.listW, this.listH)) {
            int index = (int) ((my - this.listY - 4 + this.packScroll) / PACK_ROW_HEIGHT);
            if (index == 0) {
                this.manager.clearSelection();
                Dx12ShaderPackRenderer.getInstance().invalidate();
                this.settings = List.of();
                this.playClick();
                return true;
            }
            int packIndex = index - 1;
            if (packIndex >= 0 && packIndex < this.packs.size()) {
                ShaderPackManager.PackEntry entry = this.packs.get(packIndex);
                if (entry.valid()) {
                    this.manager.select(entry.id());
                    this.manager.setEnabled(true);
                    Dx12ShaderPackRenderer.getInstance().invalidate();
                    this.settings = this.manager.currentSettings();
                    this.settingScroll = 0;
                    this.playClick();
                }
            }
            return true;
        }

        // 右侧设置项
        int settingIndex = this.settingAt(mx, my);
        if (settingIndex >= 0) {
            ShaderPack.Setting setting = this.settings.get(settingIndex);
            if (setting.isBoolean()) {
                float next = setting.numericValue() != 0.0f ? 0.0f : 1.0f;
                this.manager.setSetting(this.manager.activeId(), setting.name(), next);
                this.settings = this.manager.currentSettings();
                this.playClick();
            } else {
                this.draggingSetting = settingIndex;
                this.applySlider(setting, mx);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingSetting >= 0 && event.button() == 0
            && this.draggingSetting < this.settings.size()) {
            this.applySlider(this.settings.get(this.draggingSetting), event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingSetting >= 0) {
            this.draggingSetting = -1;
            this.settings = this.manager.currentSettings();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        int delta = (int) Math.signum(scrollY);
        if (this.isInside(x, y, this.listX, this.listY, this.listW, this.listH)) {
            this.packScroll = Mth.clamp(this.packScroll - delta * PACK_ROW_HEIGHT,
                0, this.packMaxScroll);
            return true;
        }
        if (this.isInside(x, y, this.settingsX, this.settingsY, this.settingsW,
            this.settingsH)) {
            this.settingScroll = Mth.clamp(this.settingScroll - delta * SETTING_ROW_HEIGHT,
                0, this.settingMaxScroll);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private int settingAt(double mouseX, double mouseY) {
        int listTop = this.settingsTop();
        if (mouseX < this.settingsX || mouseX >= this.settingsX + this.settingsW
            || mouseY < listTop || mouseY >= this.settingsY + this.settingsH) {
            return -1;
        }
        int index = (int) ((mouseY - listTop + this.settingScroll) / SETTING_ROW_HEIGHT);
        return index >= 0 && index < this.settings.size() ? index : -1;
    }

    private void applySlider(ShaderPack.Setting setting, double mouseX) {
        int rowRight = this.settingsX + this.settingsW - 8 - PADDING;
        int valueW = this.font.width(formatNumber(setting.numericValue()));
        int sliderX = rowRight - valueW - VALUE_GAP - SLIDER_WIDTH;
        double fraction = Mth.clamp((mouseX - sliderX) / (double) SLIDER_WIDTH, 0.0, 1.0);
        float value = (float) (setting.min() + fraction * (setting.max() - setting.min()));
        this.manager.setSetting(this.manager.activeId(), setting.name(), value);
        this.settings = this.manager.currentSettings();
    }

    private double fraction(ShaderPack.Setting setting) {
        if (setting.max() <= setting.min()) {
            return 0.0;
        }
        return Mth.clamp((setting.numericValue() - setting.min())
            / (setting.max() - setting.min()), 0.0, 1.0);
    }

    private static String formatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String tr(String chinese, String english) {
        return this.isChinese() ? chinese : english;
    }

    private boolean isChinese() {
        String code = this.minecraft.getLanguageManager().getSelected();
        return code != null && code.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    /** 按像素宽度折行；中文按字符断行，英文优先在空格处断行。 */
    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        int lastSpace = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            line.append(c);
            if (c == ' ') {
                lastSpace = line.length() - 1;
            }
            if (line.length() > 1 && this.font.width(line.toString()) > maxWidth) {
                if (lastSpace > 0) {
                    String head = line.substring(0, lastSpace);
                    String tail = line.substring(lastSpace + 1);
                    lines.add(head);
                    line.setLength(0);
                    line.append(tail);
                    lastSpace = tail.indexOf(' ');
                } else {
                    line.deleteCharAt(line.length() - 1);
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(c);
                }
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }
}
