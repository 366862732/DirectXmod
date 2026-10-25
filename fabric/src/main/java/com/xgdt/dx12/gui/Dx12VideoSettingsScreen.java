package com.xgdt.dx12.gui;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.xgdt.dx12.config.Dx12Config;
import com.xgdt.dx12.dx12.Dx12Backend;
import com.xgdt.dx12.dx12.Dx12Native;
import com.xgdt.dx12.shaderpack.ShaderPack;
import com.xgdt.dx12.shaderpack.ShaderPackManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * gl4dx12 自定义视频设置界面。
 *
 * <p>外观照搬 Reese's Sodium Options：整屏深色面板、顶部搜索栏、左侧分组导航、
 * 右侧"左标签 + 右对齐控件"的选项行，底部 Apply / Done。
 *
 * <p>选项本身仍然直接读写原版 {@link Options} 里的 {@link OptionInstance}，
 * 因此行为、存档、快捷键与重启提示全部保持不变；仅额外把"图形 API"行灰化并
 * 固定显示当前实际后端。
 */
public class Dx12VideoSettingsScreen extends VideoSettingsScreen {
    // ---- 尺寸 ----
    private static final int TOP_BAR_HEIGHT = 28;
    private static final int ROW_HEIGHT = 21;
    private static final int CONTROL_RIGHT_PADDING = 6;
    private static final int SLIDER_WIDTH = 90;
    private static final int SLIDER_HEIGHT = 10;
    private static final int SLIDER_THUMB_WIDTH = 4;
    private static final int VALUE_GAP = 6;
    private static final int CHECKBOX_SIZE = 10;

    // ---- 配色（RSO 深色主题）----
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFA0A4A8;
    private static final int ACCENT = 0xFF4FC3F7;
    private static final int ROW_BG = 0x40000000;
    private static final int ROW_BG_HOVER = 0xE0000000;
    private static final int FIELD_BG = 0x60000000;
    private static final int FIELD_BORDER = 0x30FFFFFF;
    private static final int NAV_HOVER = 0x18FFFFFF;
    private static final int NAV_SELECTED = 0x33FFFFFF;

    private final List<Row> allRows = new ArrayList<>();

    private Category category = Category.DISPLAY;
    private OptionInstance<Integer> fullscreenResolution;

    // 「图形增强」页的自有选项：直接读写 Dx12Config，不进入原版 Options。
    private OptionInstance<Integer> renderScaleOption;
    private OptionInstance<Integer> upscaleModeOption;
    private OptionInstance<Integer> upscaleSharpnessOption;
    private OptionInstance<Integer> frameGenOption;
    private OptionInstance<Integer> frameGenStrengthOption;

    /** 原生侧 NGX 状态一行文案；null = 尚未读取。 */
    private String ngxStatus;

    private int scroll;
    private int maxScroll;

    private String search = "";
    private boolean searchFocused;

    private int navX;
    private int navY;
    private int navW;
    private int navH;

    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    private int searchX;
    private int searchY;
    private int searchW;
    private int searchH;

    private int closeX;
    private int closeY;

    private int shaderButtonX;
    private int shaderButtonY;
    private int shaderButtonW;
    private int shaderButtonH;

    private int applyX;
    private int applyY;
    private int applyW;
    private int applyH;
    private int doneX;
    private int doneY;
    private int doneW;
    private int doneH;

    private Row draggingRow;

    /** DX 支持页的显卡能力数据；首次进入该页时通过 JNI 读取并缓存。 */
    private Capabilities capabilityCache;

    public Dx12VideoSettingsScreen(Screen lastScreen, Minecraft minecraft, Options options) {
        super(lastScreen, minecraft, options);
    }

    // ------------------------------------------------------------------
    // 初始化
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        this.clearWidgets();
        this.scroll = 0;
        this.searchFocused = false;
        this.draggingRow = null;
        this.ngxStatus = null;
        this.buildFullscreenResolutionOption();
        this.buildUpscalingOptions();
        this.buildRows();
    }

    /** 与原版一致的全屏分辨率选项（只存在于界面内，不写入 options 字段）。 */
    private void buildFullscreenResolutionOption() {
        Window window = this.minecraft.getWindow();
        Monitor monitor = window.findBestMonitor();
        int initialValue;
        if (monitor == null) {
            initialValue = -1;
        } else {
            Optional<VideoMode> preferred = window.getPreferredFullscreenVideoMode();
            initialValue = preferred.map(monitor::indexOfMode).orElse(-1);
        }
        this.fullscreenResolution = new OptionInstance<>("options.fullscreen.resolution",
            OptionInstance.noTooltip(), (caption, value) -> {
                if (monitor == null) {
                    return Component.translatable("options.fullscreen.unavailable");
                }
                if (value == -1) {
                    return Options.genericValueLabel(caption,
                        Component.translatable("options.fullscreen.current"));
                }
                VideoMode mode = monitor.mode(value);
                return Options.genericValueLabel(caption,
                    Component.translatable("options.fullscreen.entry", mode.getWidth(),
                        mode.getHeight(), mode.getRefreshRate(),
                        mode.getRedBits() + mode.getGreenBits() + mode.getBlueBits()));
            }, new OptionInstance.IntRange(-1, monitor != null ? monitor.modeCount() - 1 : -1),
            initialValue, value -> {
                if (monitor == null) {
                    return;
                }
                window.setPreferredFullscreenVideoMode(value == -1
                    ? Optional.empty()
                    : Optional.of(monitor.mode(value)));
            });
    }

    /**
     * 「图形增强」页的选项：内部渲染缩放 + 上采样滤镜。
     *
     * <p>这三项保存在 mod 自己的配置文件里（{@link Dx12Config}），不参与原版
     * Options 的序列化；渲染缩放由 {@code GameRendererRenderScaleMixin} 生效，
     * 上采样由原生 blit 管线在放大到窗口时执行。
     */
    private void buildUpscalingOptions() {
        Dx12Config cfg = Dx12Config.getInstance();
        this.renderScaleOption = new OptionInstance<>(
            "options.gl4dx12.render_scale", OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption,
                Component.literal(value + "%")),
            new OptionInstance.IntRange(50, 100), cfg.getRenderScale(),
            cfg::setRenderScale);
        this.upscaleModeOption = new OptionInstance<>(
            "options.gl4dx12.upscale_mode", OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption,
                Component.literal(this.upscaleModeLabel(value))),
            new OptionInstance.Enum<>(List.of(0, 1), Codec.INT), cfg.getUpscaleMode(),
            value -> {
                cfg.setUpscaleMode(value);
                Dx12Backend.pushUpscaleParams();
            });
        this.upscaleSharpnessOption = new OptionInstance<>(
            "options.gl4dx12.upscale_sharpness", OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption,
                Component.literal(value + "%")),
            new OptionInstance.IntRange(0, 100), cfg.getUpscaleSharpness(),
            value -> {
                cfg.setUpscaleSharpness(value);
                Dx12Backend.pushUpscaleParams();
            });
        this.frameGenOption = new OptionInstance<>(
            "options.gl4dx12.frame_gen", OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption,
                Component.literal(this.frameGenLabel(value))),
            new OptionInstance.Enum<>(List.of(0, 1), Codec.INT), cfg.getFrameGen(),
            value -> {
                cfg.setFrameGen(value);
                Dx12Backend.pushFrameGenParams();
            });
        this.frameGenStrengthOption = new OptionInstance<>(
            "options.gl4dx12.frame_gen_strength", OptionInstance.noTooltip(),
            (caption, value) -> Options.genericValueLabel(caption,
                Component.literal(value + "%")),
            new OptionInstance.IntRange(0, 100), cfg.getFrameGenStrength(),
            value -> {
                cfg.setFrameGenStrength(value);
                Dx12Backend.pushFrameGenParams();
            });
    }

    private String upscaleModeLabel(int mode) {
        boolean chinese = this.isChinese();
        if (mode == 1) {
            return chinese ? "CAS 锐化" : "CAS Sharpening";
        }
        return chinese ? "双线性" : "Bilinear";
    }

    /** 帧生成开关文案：0 = 关闭，1 = 开启（自研帧间插值，非 DLSS-G）。 */
    private String frameGenLabel(int value) {
        boolean chinese = this.isChinese();
        if (value == 1) {
            return chinese ? "开启（自研插帧）" : "On (Built-in)";
        }
        return chinese ? "关闭" : "Off";
    }

    /** 原生侧 NGX 状态文案（只在首次进入页面时问一次 native，随后缓存）。 */
    private String ngxStatusLine() {
        if (this.ngxStatus == null) {
            String text = "";
            if (Dx12Native.isLoaded()) {
                try {
                    text = Dx12Backend.getNgxStatus();
                } catch (Throwable t) {
                    text = "";
                }
            }
            this.ngxStatus = text == null ? "" : text;
        }
        return this.ngxStatus;
    }

    /** 「图形增强」页是否显示 NGX 状态行（搜索态与其它页不显示）。 */
    private boolean showsNgxStatusLine() {
        return this.search.isEmpty() && this.category == Category.UPSCALING
            && !this.ngxStatusLine().isEmpty();
    }

    private void buildRows() {
        this.allRows.clear();
        for (OptionInstance<?> option : this.displayOptions()) {
            this.allRows.add(new Row(option, Category.DISPLAY));
        }
        for (OptionInstance<?> option : this.qualityOptions()) {
            this.allRows.add(new Row(option, Category.QUALITY));
        }
        for (OptionInstance<?> option : this.upscalingOptions()) {
            this.allRows.add(new Row(option, Category.UPSCALING));
        }
        for (OptionInstance<?> option : this.preferenceOptions()) {
            this.allRows.add(new Row(option, Category.PREFERENCES));
        }
    }

    private List<OptionInstance<?>> upscalingOptions() {
        return List.of(this.renderScaleOption, this.upscaleModeOption,
            this.upscaleSharpnessOption, this.frameGenOption, this.frameGenStrengthOption);
    }

    private List<OptionInstance<?>> displayOptions() {
        Options o = this.options;
        return List.of(this.fullscreenResolution, o.framerateLimit(), o.enableVsync(),
            o.inactivityFpsLimit(), o.guiScale(), o.fullscreen(), o.exclusiveFullscreen(),
            o.gamma(), o.preferredGraphicsBackend());
    }

    private List<OptionInstance<?>> qualityOptions() {
        Options o = this.options;
        return List.of(o.graphicsPreset(), o.biomeBlendRadius(), o.renderDistance(),
            o.prioritizeChunkUpdates(), o.simulationDistance(), o.ambientOcclusion(),
            o.cloudStatus(), o.particles(), o.mipmapLevels(), o.entityShadows(),
            o.entityDistanceScaling(), o.menuBackgroundBlurriness(), o.cloudRange(),
            o.cutoutLeaves(), o.improvedTransparency(), o.textureFiltering(),
            o.maxAnisotropyBit(), o.weatherRadius());
    }

    private List<OptionInstance<?>> preferenceOptions() {
        Options o = this.options;
        return List.of(o.showAutosaveIndicator(), o.vignette(), o.attackIndicator(),
            o.chunkSectionFadeInTime());
    }

    /**
     * 不使用原版 OptionsList，因此跳过它的 tick 逻辑（否则会向界面塞入一个
     * 没有布局的 StringWidget）。重启用警示改为每帧直接绘制。
     */
    @Override
    public void tick() {
    }

    @Override
    protected void repositionElements() {
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // 背景保持透明：直接使用父类的世界/全景背景，不再额外蒙一层灰色。
        super.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.layout();

        // 仅在顶部栏铺一层半透明底色，整屏保持透明（可看到背后的世界/全景）。
        graphics.fill(0, 0, this.width, TOP_BAR_HEIGHT, 0x90000000);
        graphics.fill(0, TOP_BAR_HEIGHT - 1, this.width, TOP_BAR_HEIGHT, 0x30FFFFFF);

        this.drawTopBar(graphics, mouseX, mouseY);
        this.drawNav(graphics, mouseX, mouseY);
        this.drawContent(graphics, mouseX, mouseY);
        this.drawFooter(graphics, mouseX, mouseY);
    }

    private void layout() {
        this.navW = Mth.clamp(this.width / 6, 104, 170);
        this.navX = 6;
        this.navY = TOP_BAR_HEIGHT + 8;
        this.navH = Math.max(20, this.height - this.navY - 34);

        this.searchX = 8;
        this.searchY = 6;
        this.searchW = Math.min(170, Math.max(110, this.navW + 14));
        this.searchH = 16;

        this.closeX = this.width - 22;
        this.closeY = 7;

        this.contentX = this.navX + this.navW + 14;
        this.contentY = TOP_BAR_HEIGHT + 4;
        this.contentW = Math.max(120, this.width - this.contentX - 8);
        this.contentH = Math.max(40, this.height - this.contentY - 32);

        this.applyH = 18;
        this.doneH = 18;
        this.applyW = this.font.width("Apply") + 18;
        this.doneW = this.font.width(CommonComponents.GUI_DONE) + 18;
        this.doneX = this.width - 8 - this.doneW;
        this.doneY = this.height - 6 - this.doneH;
        this.applyX = this.doneX - 6 - this.applyW;
        this.applyY = this.doneY;

        this.shaderButtonW = Math.min(220, this.contentW - 40);
        this.shaderButtonH = 20;
        this.shaderButtonX = this.contentX + (this.contentW - this.shaderButtonW) / 2;
        this.shaderButtonY = this.contentY + 46;

        List<Row> shown = this.shownRows();
        if (this.isSupportPage()) {
            int visibleHeight = Math.max(0, this.contentY + this.contentH - this.supportListTop());
            this.maxScroll = Math.max(0,
                this.supportEntries().size() * ROW_HEIGHT - visibleHeight);
        } else if (this.isShaderPage()) {
            this.maxScroll = 0;
        } else {
            int visibleHeight = Math.max(0, this.contentH - 26
                - (this.options.isRestartRequiredToApplyVideoSettings() ? 12 : 0)
                - (this.showsNgxStatusLine() ? 12 : 0));
            this.maxScroll = Math.max(0, shown.size() * ROW_HEIGHT - visibleHeight);
        }
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll);
    }

    private void drawTopBar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 搜索框
        graphics.fill(this.searchX, this.searchY, this.searchX + this.searchW,
            this.searchY + this.searchH, FIELD_BG);
        graphics.outline(this.searchX, this.searchY, this.searchW, this.searchH,
            this.searchFocused ? 0x80FFFFFF : FIELD_BORDER);
        int iconX = this.searchX + 5;
        int iconY = this.searchY + 5;
        graphics.outline(iconX, iconY, 7, 7, this.searchFocused ? TEXT : TEXT_DIM);
        graphics.fill(iconX + 6, iconY + 6, iconX + 10, iconY + 7, TEXT_DIM);
        String text = this.search.isEmpty() ? "Search options..." : this.search;
        int textColor = this.search.isEmpty() ? 0xFF70757A : TEXT;
        graphics.text(this.font, text, this.searchX + 18, this.searchY + 4, textColor);
        if (this.searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = this.searchX + 18 + this.font.width(text);
            graphics.fill(caretX, this.searchY + 3, caretX + 1, this.searchY + 13, TEXT);
        }

        // 右上角：后端名称 + 关闭
        Component backend = Component.literal("DirectX 12");
        int backendW = this.font.width(backend);
        graphics.text(this.font, backend, this.closeX - 10 - backendW, this.searchY + 4, ACCENT);
        int color = this.isInside(mouseX, mouseY, this.closeX - 3, this.closeY - 2, 14, 14)
            ? TEXT : TEXT_DIM;
        for (int i = 0; i < 8; i++) {
            graphics.fill(this.closeX + i, this.closeY + i, this.closeX + i + 1,
                this.closeY + i + 1, color);
            graphics.fill(this.closeX + 7 - i, this.closeY + i, this.closeX + 8 - i,
                this.closeY + i + 1, color);
        }
    }

    private void drawNav(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(this.navX, this.navY, this.navX + this.navW, this.navY + this.navH,
            0x28000000);

        int x = this.navX + 10;
        int y = this.navY + 8;
        boolean chinese = this.isChinese();
        // 分组图标
        graphics.fill(x, y + 1, x + 10, y + 11, 0x593F8FD0);
        graphics.fill(x + 2, y + 3, x + 8, y + 9, ACCENT);
        graphics.text(this.font, "DirectXmod", x + 15, y + 1, TEXT);
        graphics.text(this.font, "26.2", x + 15, y + 12, TEXT_DIM);
        y += 30;

        for (Category cat : Category.values()) {
            boolean selected = this.search.isEmpty() && cat == this.category;
            boolean hovered = this.isInside(mouseX, mouseY, x, y, this.navW - 20, 20);
            int bg = selected ? NAV_SELECTED : hovered ? NAV_HOVER : 0;
            if (bg != 0) {
                graphics.fill(x, y, x + this.navW - 20, y + 20, bg);
            }
            if (selected) {
                graphics.fill(x, y, x + 2, y + 20, ACCENT);
            }
            graphics.text(this.font, cat.label(chinese), x + 10, y + 6, selected ? TEXT : TEXT_DIM);
            y += 22;
        }
    }

    private void drawContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.isSupportPage()) {
            this.drawSupportPage(graphics);
            return;
        }
        if (this.isShaderPage()) {
            this.drawShaderPage(graphics, mouseX, mouseY);
            return;
        }
        List<Row> shown = this.shownRows();
        boolean restart = this.options.isRestartRequiredToApplyVideoSettings();

        Component title = this.search.isEmpty()
            ? this.category.label(this.isChinese())
            : Component.literal("Search: " + this.search);
        graphics.centeredText(this.font, title, this.contentX + this.contentW / 2,
            this.contentY + 4, TEXT);
        if (restart) {
            graphics.centeredText(this.font, Component.translatable("options.restartRequired"),
                this.contentX + this.contentW / 2, this.contentY + 16, 0xFFFFD37A);
        }

        int rowTop = this.contentY + 26 + (restart ? 12 : 0);
        if (this.showsNgxStatusLine()) {
            graphics.centeredText(this.font, Component.literal(this.ngxStatusLine()),
                this.contentX + this.contentW / 2, this.contentY + 16 + (restart ? 12 : 0),
                TEXT_DIM);
            rowTop += 12;
        }
        int rowX = this.contentX;
        int rowW = this.contentW - 8;
        int rowRight = rowX + rowW;

        graphics.enableScissor(this.contentX, rowTop, this.contentX + this.contentW,
            this.contentY + this.contentH);
        for (int i = 0; i < shown.size(); i++) {
            Row row = shown.get(i);
            int y = rowTop + i * ROW_HEIGHT - this.scroll;
            if (y + ROW_HEIGHT <= rowTop || y >= this.contentY + this.contentH) {
                continue;
            }
            boolean hovered = this.isInside(mouseX, mouseY, rowX, Math.max(y, rowTop),
                rowW, ROW_HEIGHT) && mouseY >= rowTop
                && mouseY < this.contentY + this.contentH;
            this.drawRow(graphics, row, rowX, y, rowRight, hovered);
        }
        graphics.disableScissor();

        this.drawScrollBar(graphics, rowTop);
    }

    /** 右侧竖直滚动条（选项列表与 DX 支持页共用）。 */
    private void drawScrollBar(GuiGraphicsExtractor graphics, int barTop) {
        if (this.maxScroll <= 0) {
            return;
        }
        int barX = this.contentX + this.contentW - 3;
        int barHeight = Math.max(10, this.contentY + this.contentH - barTop);
        graphics.fill(barX, barTop, barX + 2, barTop + barHeight, 0x20FFFFFF);
        int thumbHeight = Math.max(12, barHeight * barHeight / (barHeight + this.maxScroll));
        int thumbY = barTop + (barHeight - thumbHeight) * this.scroll / this.maxScroll;
        graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0x80FFFFFF);
    }

    // ------------------------------------------------------------------
    // DX 支持页：读取显卡的 DX12 / DX12 Ultimate 能力并逐项展示
    // ------------------------------------------------------------------

    private boolean isSupportPage() {
        return this.search.isEmpty() && this.category == Category.DX_SUPPORT;
    }

    private boolean isShaderPage() {
        return this.search.isEmpty() && this.category == Category.SHADERS;
    }

    private void drawShaderPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean chinese = this.isChinese();
        int centerX = this.contentX + this.contentW / 2;
        graphics.centeredText(this.font, this.category.label(chinese), centerX,
            this.contentY + 4, TEXT);

        ShaderPackManager manager = ShaderPackManager.getInstance();
        ShaderPack pack = manager.activePack();
        String status;
        if (!manager.isEnabled() || pack == null) {
            status = chinese ? "光影：已关闭" : "Shaders: Disabled";
        } else {
            status = (chinese ? "当前光影：" : "Active pack: ") + pack.displayName()
                + "  (" + pack.manifest().passes().size() + (chinese ? " 个 pass)" : " passes)");
        }
        graphics.centeredText(this.font, Component.literal(status), centerX,
            this.contentY + 24, manager.isEnabled() && pack != null ? ACCENT : TEXT_DIM);

        this.drawButton(graphics,
            chinese ? "打开光影设置…" : "Open Shader Pack Settings...",
            this.shaderButtonX, this.shaderButtonY, this.shaderButtonW, this.shaderButtonH,
            this.isInside(mouseX, mouseY, this.shaderButtonX, this.shaderButtonY,
                this.shaderButtonW, this.shaderButtonH));

        String hint = chinese
            ? "光影包（.zip）放在 shaderpacks/ 目录，支持 Iris 风格的 shaders.properties。"
            : "Shader packs (.zip) live in shaderpacks/ and use Iris-style shaders.properties.";
        int y = this.shaderButtonY + this.shaderButtonH + 12;
        for (String line : this.wrapText(hint, Math.max(80, this.contentW - 20))) {
            graphics.centeredText(this.font, Component.literal(line), centerX, y, TEXT_DIM);
            y += this.font.lineHeight + 1;
        }
    }

    /** 跟随游戏语言：zh* 显示中文，其余显示英文。 */
    private boolean isChinese() {
        String code = this.minecraft.getLanguageManager().getSelected();
        return code != null && code.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    private Capabilities capabilities() {
        if (this.capabilityCache == null) {
            this.capabilityCache = Capabilities.load();
        }
        return this.capabilityCache;
    }

    private String supportHeadline() {
        boolean chinese = this.isChinese();
        Capabilities caps = this.capabilities();
        if (!caps.available) {
            if ("DX12".equals(caps.backend)) {
                return chinese ? "无法读取 DirectX 12 能力信息。"
                    : "Unable to read DirectX 12 capability information.";
            }
            return chinese
                ? "当前未运行在 DirectX 12 后端（" + caps.backend + "），无法读取 DX12 特性。"
                : "DirectX 12 backend is not active (" + caps.backend
                    + "); DX12 features are unavailable.";
        }
        if (caps.fullySupportsUltimate()) {
            return chinese ? "恭喜你！你的显卡完全支持 DX12 的所有特性！"
                : "Congratulations! Your GPU fully supports all DirectX 12 features!";
        }
        return chinese
            ? "你的显卡支持到 DirectX " + caps.versionLabel() + "，部分 DX12 Ultimate 特性不可用。"
            : "Your GPU supports up to DirectX " + caps.versionLabel()
                + "; some DX12 Ultimate features are unavailable.";
    }

    private List<String> supportHeadlineLines() {
        return this.wrapText(this.supportHeadline(), Math.max(80, this.contentW - 20));
    }

    /** 标题 + 结论 + 显卡名占用的高度。 */
    private int supportHeaderHeight() {
        Capabilities caps = this.capabilities();
        int height = 18;
        height += this.supportHeadlineLines().size() * (this.font.lineHeight + 1);
        if (caps.available && !caps.adapter.isEmpty()) {
            height += this.font.lineHeight + 4;
        }
        return height + 6;
    }

    private int supportListTop() {
        return Math.min(this.contentY + this.supportHeaderHeight(),
            this.contentY + this.contentH);
    }

    private List<SupportEntry> supportEntries() {
        Capabilities caps = this.capabilities();
        boolean chinese = this.isChinese();
        List<SupportEntry> entries = new ArrayList<>();
        if (!caps.available) {
            return entries;
        }
        entries.add(SupportEntry.header(chinese ? "Direct3D 版本" : "Direct3D Versions"));
        entries.add(SupportEntry.feature("DirectX 12 (FL 11_0)", "",
            caps.featureLevel() >= 110));
        entries.add(SupportEntry.feature("DirectX 12.1 (FL 12_1)", "",
            caps.featureLevel() >= 121));
        entries.add(SupportEntry.feature("DirectX 12.2 Ultimate (FL 12_2)", "",
            caps.featureLevel() >= 122));

        entries.add(SupportEntry.header("DX12 Ultimate"));
        entries.add(SupportEntry.feature("DirectX Raytracing (DXR) 1.0", "",
            caps.raytracingTier >= 10));
        entries.add(SupportEntry.feature("DirectX Raytracing (DXR) 1.1", "",
            caps.raytracingTier >= 11));
        entries.add(SupportEntry.feature("DirectX Raytracing (DXR) 1.2", "",
            caps.raytracingTier >= 12));
        entries.add(SupportEntry.feature(chinese ? "网格着色器 Mesh Shader" : "Mesh Shader",
            caps.meshShaderTier > 0 ? "Tier 1" : "-", caps.meshShaderTier > 0));
        entries.add(SupportEntry.feature(
            chinese ? "可变速率着色 VRS" : "Variable Rate Shading (VRS)",
            caps.vrsTier > 0 ? "Tier " + caps.vrsTier : "-", caps.vrsTier > 0));
        entries.add(SupportEntry.feature(
            chinese ? "采样器反馈 Sampler Feedback" : "Sampler Feedback",
            caps.samplerFeedbackTier >= 100 ? "1.0"
                : caps.samplerFeedbackTier >= 90 ? "0.9" : "-",
            caps.samplerFeedbackTier > 0));

        entries.add(SupportEntry.header(chinese ? "本模组光追管线" : "Mod RT Pipeline"));
        entries.add(SupportEntry.feature("BLAS / TLAS / RTPSO / DispatchRays", "",
            caps.device5));
        return entries;
    }

    private void drawSupportPage(GuiGraphicsExtractor graphics) {
        boolean chinese = this.isChinese();
        Capabilities caps = this.capabilities();
        int centerX = this.contentX + this.contentW / 2;

        graphics.centeredText(this.font, this.category.label(chinese),
            centerX, this.contentY + 4, TEXT);

        int color = !caps.available ? 0xFFE05C5C
            : caps.fullySupportsUltimate() ? 0xFF7ED957 : 0xFFFFD37A;
        int y = this.contentY + 18;
        for (String line : this.supportHeadlineLines()) {
            graphics.centeredText(this.font, Component.literal(line), centerX, y, color);
            y += this.font.lineHeight + 1;
        }
        if (caps.available && !caps.adapter.isEmpty()) {
            graphics.centeredText(this.font,
                Component.literal((chinese ? "显卡：" : "GPU: ") + caps.adapter),
                centerX, y + 3, TEXT_DIM);
        }

        int listTop = this.supportListTop();
        int listBottom = this.contentY + this.contentH;
        int rowX = this.contentX;
        int rowRight = rowX + this.contentW - 8;
        List<SupportEntry> entries = this.supportEntries();

        graphics.enableScissor(this.contentX, listTop, this.contentX + this.contentW, listBottom);
        for (int i = 0; i < entries.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT - this.scroll;
            if (rowY + ROW_HEIGHT <= listTop || rowY >= listBottom) {
                continue;
            }
            this.drawSupportEntry(graphics, entries.get(i), rowX, rowY, rowRight);
        }
        graphics.disableScissor();

        this.drawScrollBar(graphics, listTop);
    }

    private void drawSupportEntry(GuiGraphicsExtractor graphics, SupportEntry entry,
                                  int x, int y, int rowRight) {
        int textY = y + (ROW_HEIGHT - this.font.lineHeight) / 2;
        if (entry.header()) {
            graphics.text(this.font, entry.label(), x + CONTROL_RIGHT_PADDING, textY, TEXT_DIM);
            return;
        }
        graphics.fill(x, y, rowRight, y + ROW_HEIGHT, ROW_BG);
        graphics.text(this.font, entry.label(), x + CONTROL_RIGHT_PADDING, textY, TEXT);

        int controlRight = rowRight - CONTROL_RIGHT_PADDING;
        int iconColor = entry.supported() ? 0xFF7ED957 : 0xFFE05C5C;
        int iconX = controlRight - 8;
        this.drawStatusIcon(graphics, iconX, y + (ROW_HEIGHT - 8) / 2, entry.supported(),
            iconColor);
        if (!entry.value().isEmpty()) {
            graphics.text(this.font, entry.value(),
                iconX - 6 - this.font.width(entry.value()), textY, TEXT_DIM);
        }
    }

    /** 手绘 8×8 对勾/叉号（默认字体不含 U+2713/U+2717 字形）。 */
    private void drawStatusIcon(GuiGraphicsExtractor graphics, int x, int y, boolean ok,
                                int color) {
        if (ok) {
            graphics.fill(x, y + 4, x + 2, y + 6, color);
            graphics.fill(x + 2, y + 6, x + 4, y + 8, color);
            graphics.fill(x + 4, y + 4, x + 6, y + 6, color);
            graphics.fill(x + 6, y + 2, x + 8, y + 4, color);
            return;
        }
        graphics.fill(x, y, x + 2, y + 2, color);
        graphics.fill(x + 2, y + 2, x + 4, y + 4, color);
        graphics.fill(x + 4, y + 4, x + 6, y + 6, color);
        graphics.fill(x + 6, y + 6, x + 8, y + 8, color);
        graphics.fill(x + 6, y, x + 8, y + 2, color);
        graphics.fill(x + 4, y + 2, x + 6, y + 4, color);
        graphics.fill(x + 2, y + 4, x + 4, y + 6, color);
        graphics.fill(x, y + 6, x + 2, y + 8, color);
    }

    /** 按像素宽度折行；中文按字符断行，英文优先在空格处断行。 */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
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
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void drawRow(GuiGraphicsExtractor graphics, Row row, int x, int y,
                         int rowRight, boolean hovered) {
        boolean enabled = row.enabled();
        graphics.fill(x, y, rowRight, y + ROW_HEIGHT,
            hovered && enabled ? ROW_BG_HOVER : ROW_BG);
        int textY = y + (ROW_HEIGHT - this.font.lineHeight) / 2;
        graphics.text(this.font, row.label, x + CONTROL_RIGHT_PADDING, textY,
            enabled ? TEXT : TEXT_DIM);

        int controlRight = rowRight - CONTROL_RIGHT_PADDING;
        switch (row.kind) {
            case READONLY -> {
                Component value = backendLabel();
                graphics.text(this.font, value, controlRight - this.font.width(value), textY,
                    TEXT_DIM);
            }
            case BOOLEAN -> this.drawCheckbox(graphics, controlRight, y, (Boolean) row.option.get(),
                enabled);
            case CYCLE -> {
                Component value = row.valueText();
                graphics.text(this.font, value, controlRight - this.font.width(value), textY,
                    enabled ? TEXT : TEXT_DIM);
            }
            case SLIDER -> {
                boolean showSlider = enabled
                    && (hovered || this.draggingRow == row);
                Component value = row.valueText();
                int valueW = this.font.width(value);
                if (showSlider) {
                    int valueX = controlRight - valueW;
                    graphics.text(this.font, value, valueX, textY, TEXT);
                    int sliderX = valueX - VALUE_GAP - SLIDER_WIDTH;
                    int trackY = y + ROW_HEIGHT / 2;
                    graphics.fill(sliderX, trackY, sliderX + SLIDER_WIDTH, trackY + 1, 0x66FFFFFF);
                    int thumbX = sliderX
                        + (int) (row.sliderFraction() * SLIDER_WIDTH) - SLIDER_THUMB_WIDTH / 2;
                    graphics.fill(thumbX, trackY - SLIDER_HEIGHT / 2,
                        thumbX + SLIDER_THUMB_WIDTH, trackY + SLIDER_HEIGHT / 2, TEXT);
                } else {
                    graphics.text(this.font, value, controlRight - valueW, textY,
                        enabled ? TEXT : TEXT_DIM);
                }
            }
        }
    }

    private void drawCheckbox(GuiGraphicsExtractor graphics, int controlRight, int rowY,
                              boolean checked, boolean enabled) {
        int x = controlRight - CHECKBOX_SIZE;
        int y = rowY + (ROW_HEIGHT - CHECKBOX_SIZE) / 2;
        // 复用 Reese's Sodium Options 的小方框样式：外圈实心描边 + 中间留缝，
        // 选中时中心再实心填充。比此前 1px 描边在深色背景上清晰得多。
        int color = enabled ? ACCENT : 0xFF6A7078;
        graphics.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, color);
        graphics.fill(x + 2, y + 2, x + CHECKBOX_SIZE - 2, y + CHECKBOX_SIZE - 2,
            0xFF14171A);
        if (checked) {
            graphics.fill(x + 3, y + 3, x + CHECKBOX_SIZE - 3, y + CHECKBOX_SIZE - 3, color);
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.drawButton(graphics, "Apply", this.applyX, this.applyY, this.applyW, this.applyH,
            this.isInside(mouseX, mouseY, this.applyX, this.applyY, this.applyW, this.applyH));
        this.drawButton(graphics, CommonComponents.GUI_DONE.getString(), this.doneX, this.doneY,
            this.doneW, this.doneH,
            this.isInside(mouseX, mouseY, this.doneX, this.doneY, this.doneW, this.doneH));
    }

    private void drawButton(GuiGraphicsExtractor graphics, String label, int x, int y, int w,
                            int h, boolean hovered) {
        graphics.fill(x, y, x + w, y + h, hovered ? 0xE0000000 : 0x60000000);
        graphics.outline(x, y, w, h, hovered ? 0x60FFFFFF : 0x30FFFFFF);
        graphics.centeredText(this.font, label, x + w / 2,
            y + (h - this.font.lineHeight) / 2, TEXT);
    }

    // ------------------------------------------------------------------
    // 鼠标 / 键盘
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (this.isInside(mx, my, this.searchX, this.searchY, this.searchW, this.searchH)) {
            this.searchFocused = true;
            return true;
        }
        this.searchFocused = false;

        if (this.isInside(mx, my, this.closeX - 3, this.closeY - 2, 14, 14)) {
            this.onClose();
            return true;
        }
        if (this.isInside(mx, my, this.applyX, this.applyY, this.applyW, this.applyH)) {
            this.options.save();
            return true;
        }
        if (this.isInside(mx, my, this.doneX, this.doneY, this.doneW, this.doneH)) {
            this.onClose();
            return true;
        }

        // 左侧导航
        int navItemX = this.navX + 10;
        int navItemY = this.navY + 8 + 30;
        if (this.search.isEmpty() && mx >= navItemX && mx < navItemX + this.navW - 20) {
            for (Category cat : Category.values()) {
                if (my >= navItemY && my < navItemY + 20) {
                    this.category = cat;
                    this.scroll = 0;
                    if (cat == Category.SHADERS) {
                        this.minecraft.setScreenAndShow(new Dx12ShaderPackScreen(this));
                    }
                    return true;
                }
                navItemY += 22;
            }
        }

        if (this.isShaderPage() && this.isInside(mx, my, this.shaderButtonX,
            this.shaderButtonY, this.shaderButtonW, this.shaderButtonH)) {
            this.minecraft.setScreenAndShow(new Dx12ShaderPackScreen(this));
            return true;
        }

        Row row = this.rowAt(mx, my);
        if (row == null || !row.enabled()) {
            return true;
        }
        switch (row.kind) {
            case BOOLEAN -> {
                row.setValue(!(Boolean) row.option.get());
                this.playClick();
            }
            case CYCLE -> {
                row.cycle(!this.minecraft.hasShiftDown());
                this.playClick();
            }
            case SLIDER -> {
                int sliderX = row.sliderX();
                if (mx >= sliderX && mx < sliderX + SLIDER_WIDTH) {
                    this.draggingRow = row;
                    row.setValueFromMouse(mx);
                }
            }
            default -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingRow != null && event.button() == 0) {
            this.draggingRow.setValueFromMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingRow != null) {
            this.draggingRow = null;
            this.options.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x >= this.contentX && x < this.contentX + this.contentW
            && y >= this.contentY && y < this.contentY + this.contentH) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY) * ROW_HEIGHT,
                0, this.maxScroll);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchFocused) {
            if (event.isEscape()) {
                this.search = "";
                this.searchFocused = false;
                this.scroll = 0;
                return true;
            }
            if (event.key() == 259) {
                if (!this.search.isEmpty()) {
                    this.search = this.search.substring(0, this.search.length() - 1);
                    this.scroll = 0;
                }
                return true;
            }
            if (event.key() == 257 || event.key() == 335) {
                this.searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchFocused && event.isAllowedChatCharacter()) {
            this.search += event.codepointAsString();
            this.scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    // ------------------------------------------------------------------
    // 行数据
    // ------------------------------------------------------------------

    private List<Row> shownRows() {
        if (this.search.isBlank()) {
            return this.allRows.stream().filter(row -> row.category == this.category).toList();
        }
        String query = this.search.toLowerCase(Locale.ROOT);
        return this.allRows.stream()
            .filter(row -> row.label.toLowerCase(Locale.ROOT).contains(query))
            .toList();
    }

    private Row rowAt(double mouseX, double mouseY) {
        if (mouseX < this.contentX || mouseX >= this.contentX + this.contentW
            || mouseY < this.contentY + 26 || mouseY >= this.contentY + this.contentH) {
            return null;
        }
        List<Row> shown = this.shownRows();
        int rowTop = this.contentY + 26
            + (this.options.isRestartRequiredToApplyVideoSettings() ? 12 : 0);
        for (int i = 0; i < shown.size(); i++) {
            int y = rowTop + i * ROW_HEIGHT - this.scroll;
            if (mouseY >= y && mouseY < y + ROW_HEIGHT) {
                return shown.get(i);
            }
        }
        return null;
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private final class Row {
        private final OptionInstance<?> option;
        private final Category category;
        private final Kind kind;
        private final String label;

        private AbstractWidget probe;
        private Object probeValue;

        private Row(OptionInstance<?> option, Category category) {
            this.option = option;
            this.category = category;
            this.kind = this.classify();
            this.label = option.toString();
        }

        private Kind classify() {
            if (this.option == Dx12VideoSettingsScreen.this.options.preferredGraphicsBackend()) {
                return Kind.READONLY;
            }
            if (this.option.get() instanceof Boolean) {
                return Kind.BOOLEAN;
            }
            OptionInstance.ValueSet<?> values = this.option.values();
            if (values instanceof OptionInstance.SliderableOrCyclableValueSet<?> set) {
                return set.createCycleButton() ? Kind.CYCLE : Kind.SLIDER;
            }
            if (values instanceof OptionInstance.CycleableValueSet<?>) {
                return Kind.CYCLE;
            }
            if (values instanceof OptionInstance.SliderableValueSet<?>) {
                return Kind.SLIDER;
            }
            return Kind.CYCLE;
        }

        private boolean enabled() {
            if (this.kind == Kind.READONLY) {
                return false;
            }
            if (this.option == Dx12VideoSettingsScreen.this.options.maxAnisotropyBit()) {
                return Dx12VideoSettingsScreen.this.options.textureFiltering().get()
                    == TextureFilteringMethod.ANISOTROPIC;
            }
            // 锐化强度仅在上采样模式为 CAS 时生效。
            if (this.option == Dx12VideoSettingsScreen.this.upscaleSharpnessOption) {
                return Dx12VideoSettingsScreen.this.upscaleModeOption.get() == 1;
            }
            // 插值权重仅在帧生成开启时生效。
            if (this.option == Dx12VideoSettingsScreen.this.frameGenStrengthOption) {
                return Dx12VideoSettingsScreen.this.frameGenOption.get() == 1;
            }
            return true;
        }

        /**
         * 借用原版控件生成"当前值"的本地化文本，再裁掉前缀的选项名，
         * 这样不需要自己维护每种选项的格式化逻辑。
         */
        private Component valueText() {
            Object current = this.option.get();
            if (this.probe == null || !Objects.equals(this.probeValue, current)) {
                this.probe = this.option.createButton(
                    Dx12VideoSettingsScreen.this.options, 0, 0, 100);
                this.probeValue = current;
            }
            String message = this.probe.getMessage().getString();
            return Component.literal(stripCaption(message, this.label));
        }

        private int sliderX() {
            int controlRight = Dx12VideoSettingsScreen.this.contentX
                + Dx12VideoSettingsScreen.this.contentW - 8 - CONTROL_RIGHT_PADDING;
            int valueW = Dx12VideoSettingsScreen.this.font.width(this.valueText());
            return controlRight - valueW - VALUE_GAP - SLIDER_WIDTH;
        }

        @SuppressWarnings("rawtypes")
        private double sliderFraction() {
            OptionInstance.SliderableValueSet values =
                (OptionInstance.SliderableValueSet) this.option.values();
            return values.toSliderValue(this.option.get());
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void setValueFromMouse(double mouseX) {
            double fraction = Mth.clamp((mouseX - this.sliderX()) / (double) SLIDER_WIDTH,
                0.0, 1.0);
            OptionInstance.SliderableValueSet values =
                (OptionInstance.SliderableValueSet) this.option.values();
            this.setValue(values.fromSliderValue(fraction));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void setValue(Object value) {
            ((OptionInstance) this.option).set(value);
            this.probeValue = null;
        }

        private void cycle(boolean backwards) {
            Object current = this.option.get();
            OptionInstance.ValueSet<?> values = this.option.values();
            if (values instanceof OptionInstance.CycleableValueSet<?> cycleable) {
                List<?> list = cycleable.valueListSupplier().getSelectedList();
                int index = list.indexOf(current);
                if (index < 0 || list.isEmpty()) {
                    return;
                }
                int next = backwards
                    ? (index - 1 + list.size()) % list.size()
                    : (index + 1) % list.size();
                this.setValue(list.get(next));
                return;
            }
            if (values instanceof OptionInstance.SliderableValueSet) {
                @SuppressWarnings("rawtypes")
                OptionInstance.SliderableValueSet sliderable =
                    (OptionInstance.SliderableValueSet) values;
                Optional<?> next = backwards ? sliderable.previous(current)
                    : sliderable.next(current);
                next.ifPresent(this::setValue);
            }
        }
    }

    private enum Kind {
        BOOLEAN, CYCLE, SLIDER, READONLY
    }

    private enum Category {
        DISPLAY("options.video.display.header", "显示", "Display"),
        QUALITY("options.video.quality.header", "质量", "Quality"),
        UPSCALING(null, "图形增强", "Graphics Enhancements"),
        PREFERENCES("options.video.preferences.header", "偏好", "Preferences"),
        SHADERS(null, "光影", "Shaders"),
        DX_SUPPORT(null, "DX 支持", "DX Support");

        private final String translationKey;
        private final String chineseLabel;
        private final String englishLabel;

        Category(String translationKey, String chineseLabel, String englishLabel) {
            this.translationKey = translationKey;
            this.chineseLabel = chineseLabel;
            this.englishLabel = englishLabel;
        }

        private Component label(boolean chinese) {
            if (this.translationKey == null) {
                return Component.literal(chinese ? this.chineseLabel : this.englishLabel);
            }
            return Component.translatable(this.translationKey);
        }
    }

    /** DX 支持页的一行：分组标题 / 带对勾叉号的能力项。 */
    private record SupportEntry(String label, String value, boolean header, boolean supported) {
        private static SupportEntry header(String label) {
            return new SupportEntry(label, "", true, true);
        }

        private static SupportEntry feature(String label, String value, boolean supported) {
            return new SupportEntry(label, value, false, supported);
        }
    }

    /** 解析 dx12GetCapabilities() 的 "key=value;..." 摘要。 */
    private static final class Capabilities {
        private final boolean available;
        private final String backend;
        private final String adapter;
        private final int featureLevelMajor;
        private final int featureLevelMinor;
        /** 0=不支持 / 10=DXR 1.0 / 11=DXR 1.1 */
        private final int raytracingTier;
        private final boolean device5;
        /** 0=不支持 / 10=Tier 1 */
        private final int meshShaderTier;
        private final int vrsTier;
        /** 0 / 90(0.9) / 100(1.0) */
        private final int samplerFeedbackTier;

        private Capabilities(boolean available, String backend, String adapter,
                             int featureLevelMajor, int featureLevelMinor, int raytracingTier,
                             boolean device5, int meshShaderTier, int vrsTier,
                             int samplerFeedbackTier) {
            this.available = available;
            this.backend = backend;
            this.adapter = adapter;
            this.featureLevelMajor = featureLevelMajor;
            this.featureLevelMinor = featureLevelMinor;
            this.raytracingTier = raytracingTier;
            this.device5 = device5;
            this.meshShaderTier = meshShaderTier;
            this.vrsTier = vrsTier;
            this.samplerFeedbackTier = samplerFeedbackTier;
        }

        private static Capabilities load() {
            String backend = "unknown";
            String adapter = "";
            try {
                var info = RenderSystem.getDevice().getDeviceInfo();
                backend = info.backendName();
                adapter = info.name();
            } catch (Throwable ignored) {
                // 设备信息不可用时保留默认值，下面按"不可用"展示。
            }
            if (!"DX12".equals(backend)) {
                return unavailable(backend, adapter);
            }
            try {
                return parse(Dx12Native.dx12GetCapabilities(), backend, adapter);
            } catch (Throwable t) {
                return unavailable(backend, adapter);
            }
        }

        private static Capabilities unavailable(String backend, String adapter) {
            return new Capabilities(false, backend, adapter, 0, 0, 0, false, 0, 0, 0);
        }

        private static Capabilities parse(String raw, String backend, String adapter) {
            Map<String, String> values = new HashMap<>();
            if (raw != null) {
                for (String part : raw.split(";")) {
                    int eq = part.indexOf('=');
                    if (eq > 0) {
                        values.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                    }
                }
            }
            int[] level = parsePair(values.getOrDefault("featureLevel", "0_0"), '_');
            int[] rt = parsePair(values.getOrDefault("raytracing", "0.0"), '.');
            int raytracing = rt[0] * 10 + rt[1];
            boolean device5 = "1".equals(values.getOrDefault("device5", "0"));
            int meshShader = "none".equalsIgnoreCase(values.getOrDefault("meshShader", "none"))
                ? 0 : 10;
            int vrs = parseInt(values.getOrDefault("vrs", "0"), 0);
            String feedback = values.getOrDefault("samplerFeedback", "none");
            int samplerFeedback = "1.0".equals(feedback) ? 100
                : "0.9".equals(feedback) ? 90 : 0;
            return new Capabilities(true, backend, adapter, level[0], level[1], raytracing,
                device5, meshShader, vrs, samplerFeedback);
        }

        /** 按分隔符拆分两个整数；不能用 String.split，"." 在正则里是通配符。 */
        private static int[] parsePair(String text, char separator) {
            int index = text.indexOf(separator);
            if (index < 0) {
                return new int[]{0, 0};
            }
            return new int[]{parseInt(text.substring(0, index), 0),
                parseInt(text.substring(index + 1), 0)};
        }

        private static int parseInt(String text, int fallback) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        /** FL 11_0 → 110，FL 12_2 → 122，便于比较。 */
        private int featureLevel() {
            return this.featureLevelMajor * 10 + this.featureLevelMinor;
        }

        private String versionLabel() {
            return this.featureLevelMajor + "." + this.featureLevelMinor;
        }

        /** DX12 Ultimate 要素（DXR / Mesh Shader / VRS / Sampler Feedback）+ Device5 全部具备。 */
        private boolean fullySupportsUltimate() {
            return this.available
                && this.featureLevel() >= 122
                && this.raytracingTier >= 11
                && this.meshShaderTier > 0
                && this.vrsTier > 0
                && this.samplerFeedbackTier > 0
                && this.device5;
        }
    }

    /** 去掉原版控件文本里的 "选项名: " 前缀。 */
    private static String stripCaption(String message, String caption) {
        if (message.equals(caption)) {
            return "";
        }
        if (message.startsWith(caption)) {
            String rest = message.substring(caption.length()).stripLeading();
            if (rest.startsWith(":") || rest.startsWith("：")) {
                rest = rest.substring(1).stripLeading();
            }
            return rest;
        }
        return message;
    }

    /** 当前实际后端名（DX12 显示为 DirectX 12）。 */
    private static Component backendLabel() {
        String name = RenderSystem.getDevice().getDeviceInfo().backendName();
        return Component.literal("DX12".equals(name) ? "DirectX 12" : name);
    }
}
