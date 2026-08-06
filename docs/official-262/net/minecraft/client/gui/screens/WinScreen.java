/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.ints.IntOpenHashSet
 *  it.unimi.dsi.fastutil.ints.IntSet
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.apache.commons.lang3.StringUtils
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class WinScreen
extends Screen {
    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/credits_vignette.png");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
    private static final String NAME_PREFIX = "           ";
    private static final String OBFUSCATE_TOKEN = String.valueOf((Object)ChatFormatting.WHITE) + String.valueOf((Object)ChatFormatting.OBFUSCATED) + String.valueOf((Object)ChatFormatting.GREEN) + String.valueOf((Object)ChatFormatting.AQUA);
    private static final float SPEEDUP_FACTOR = 5.0f;
    private static final float SPEEDUP_FACTOR_FAST = 15.0f;
    private static final Identifier END_POEM_LOCATION = Identifier.withDefaultNamespace("texts/end.txt");
    private static final Identifier CREDITS_LOCATION = Identifier.withDefaultNamespace("texts/credits.json");
    private static final Identifier POSTCREDITS_LOCATION = Identifier.withDefaultNamespace("texts/postcredits.txt");
    private final boolean poem;
    private final Runnable onFinished;
    private float scroll;
    private List<FormattedCharSequence> lines;
    private List<Component> narratorComponents;
    private IntSet centeredLines;
    private int totalScrollLength;
    private boolean speedupActive;
    private final IntSet speedupModifiers = new IntOpenHashSet();
    private float scrollSpeed;
    private final float unmodifiedScrollSpeed;
    private int direction;
    private final LogoRenderer logoRenderer = new LogoRenderer(false);

    public WinScreen(boolean poem, Runnable onFinished) {
        super(GameNarrator.NO_TITLE);
        this.poem = poem;
        this.onFinished = onFinished;
        this.unmodifiedScrollSpeed = !poem ? 0.75f : 0.5f;
        this.direction = 1;
        this.scrollSpeed = this.unmodifiedScrollSpeed;
    }

    private float calculateScrollSpeed() {
        if (this.speedupActive) {
            return this.unmodifiedScrollSpeed * (5.0f + (float)this.speedupModifiers.size() * 15.0f) * (float)this.direction;
        }
        return this.unmodifiedScrollSpeed * (float)this.direction;
    }

    @Override
    public void tick() {
        this.minecraft.getMusicManager().tick();
        this.minecraft.getSoundManager().tick(false);
        float maxScroll = this.totalScrollLength + this.height + this.height + 24;
        if (this.scroll > maxScroll) {
            this.respawn();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isUp()) {
            this.direction = -1;
        } else if (event.key() == 341 || event.key() == 345) {
            this.speedupModifiers.add(event.key());
        } else if (event.key() == 32) {
            this.speedupActive = true;
        }
        this.scrollSpeed = this.calculateScrollSpeed();
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.isUp()) {
            this.direction = 1;
        }
        if (event.key() == 32) {
            this.speedupActive = false;
        } else if (event.key() == 341 || event.key() == 345) {
            this.speedupModifiers.remove(event.key());
        }
        this.scrollSpeed = this.calculateScrollSpeed();
        return super.keyReleased(event);
    }

    @Override
    public void onClose() {
        this.respawn();
    }

    private void respawn() {
        this.onFinished.run();
    }

    @Override
    protected void init() {
        if (this.lines != null) {
            return;
        }
        this.lines = Lists.newArrayList();
        this.narratorComponents = Lists.newArrayList();
        this.centeredLines = new IntOpenHashSet();
        if (this.poem) {
            this.wrapCreditsIO(END_POEM_LOCATION, this::addPoemFile);
        }
        this.wrapCreditsIO(CREDITS_LOCATION, this::addCreditsFile);
        if (this.poem) {
            this.wrapCreditsIO(POSTCREDITS_LOCATION, this::addPoemFile);
        }
        this.totalScrollLength = this.lines.size() * 12;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration((Component[])this.narratorComponents.toArray(Component[]::new));
    }

    private void wrapCreditsIO(Identifier file, CreditsReader creditsReader) {
        try (BufferedReader resource = this.minecraft.getResourceManager().openAsReader(file);){
            creditsReader.read(resource);
        }
        catch (Exception e) {
            LOGGER.error("Couldn't load credits from file {}", (Object)file, (Object)e);
        }
    }

    private void addPoemFile(Reader inputReader) throws IOException {
        Object line;
        BufferedReader reader = new BufferedReader(inputReader);
        RandomSource random = RandomSource.createThreadLocalInstance(8124371L);
        while ((line = reader.readLine()) != null) {
            int pos;
            line = ((String)line).replaceAll("PLAYERNAME", this.minecraft.getUser().getName());
            while ((pos = ((String)line).indexOf(OBFUSCATE_TOKEN)) != -1) {
                String before = ((String)line).substring(0, pos);
                String after = ((String)line).substring(pos + OBFUSCATE_TOKEN.length());
                line = before + String.valueOf((Object)ChatFormatting.WHITE) + String.valueOf((Object)ChatFormatting.OBFUSCATED) + "XXXXXXXX".substring(0, random.nextInt(4) + 3) + after;
            }
            this.addPoemLines((String)line);
            this.addEmptyLine();
        }
        for (int i = 0; i < 8; ++i) {
            this.addEmptyLine();
        }
    }

    private void addCreditsFile(Reader inputReader) {
        JsonArray root = GsonHelper.parseArray(inputReader);
        for (JsonElement sectionElement : root) {
            JsonObject section = sectionElement.getAsJsonObject();
            String sectionName = section.get("section").getAsString();
            this.addCreditsLine(SECTION_HEADING, true, false);
            this.addCreditsLine(Component.literal(sectionName).withStyle(ChatFormatting.YELLOW), true, true);
            this.addCreditsLine(SECTION_HEADING, true, false);
            this.addEmptyLine();
            this.addEmptyLine();
            JsonArray disciplines = section.getAsJsonArray("disciplines");
            for (JsonElement disciplineElement : disciplines) {
                JsonObject discipline = disciplineElement.getAsJsonObject();
                String disciplineName = discipline.get("discipline").getAsString();
                if (StringUtils.isNotEmpty((CharSequence)disciplineName)) {
                    this.addCreditsLine(Component.literal(disciplineName).withStyle(ChatFormatting.YELLOW), true, true);
                    this.addEmptyLine();
                    this.addEmptyLine();
                }
                JsonArray titles = discipline.getAsJsonArray("titles");
                for (JsonElement titleElement : titles) {
                    JsonObject title = titleElement.getAsJsonObject();
                    String titleName = title.get("title").getAsString();
                    JsonArray names = title.getAsJsonArray("names");
                    this.addCreditsLine(Component.literal(titleName).withStyle(ChatFormatting.GRAY), false, true);
                    for (JsonElement nameElement : names) {
                        String name = nameElement.getAsString();
                        this.addCreditsLine(Component.literal(NAME_PREFIX).append(name).withStyle(ChatFormatting.WHITE), false, true);
                    }
                    this.addEmptyLine();
                    this.addEmptyLine();
                }
            }
        }
    }

    private void addEmptyLine() {
        this.lines.add(FormattedCharSequence.EMPTY);
        this.narratorComponents.add(CommonComponents.EMPTY);
    }

    private void addPoemLines(String line) {
        MutableComponent component = Component.literal(line);
        this.lines.addAll(this.minecraft.font.split(component, 256));
        this.narratorComponents.add(component);
    }

    private void addCreditsLine(Component line, boolean centered, boolean narrated) {
        if (centered) {
            this.centeredLines.add(this.lines.size());
        }
        this.lines.add(line.getVisualOrderText());
        if (narrated) {
            this.narratorComponents.add(line);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractVignette(graphics);
        this.scroll = Math.max(0.0f, this.scroll + a * this.scrollSpeed);
        int logoX = this.width / 2 - 128;
        int logoY = this.height + 50;
        float yOffs = -this.scroll;
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0f, yOffs);
        graphics.nextStratum();
        this.logoRenderer.extractRenderState(graphics, this.width, 1.0f, logoY);
        int yPos = logoY + 100;
        for (int i = 0; i < this.lines.size(); ++i) {
            float diff;
            if (i == this.lines.size() - 1 && (diff = (float)yPos + yOffs - (float)(this.height / 2 - 6)) < 0.0f) {
                graphics.pose().translate(0.0f, -diff);
            }
            if ((float)yPos + yOffs + 12.0f + 8.0f > 0.0f && (float)yPos + yOffs < (float)this.height) {
                FormattedCharSequence line = this.lines.get(i);
                if (this.centeredLines.contains(i)) {
                    graphics.centeredText(this.font, line, logoX + 128, yPos, -1);
                } else {
                    graphics.text(this.font, line, logoX, yPos, -1);
                }
            }
            yPos += 12;
        }
        graphics.pose().popMatrix();
    }

    private void extractVignette(GuiGraphicsExtractor graphics) {
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE_LOCATION, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.poem) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_SKY_LOCATION);
            AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalRenderer.END_PORTAL_LOCATION);
            TextureSetup textureSetup = TextureSetup.doubleTexture(skyTexture.getTextureView(), skyTexture.getSampler(), portalTexture.getTextureView(), portalTexture.getSampler());
            graphics.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
        } else {
            super.extractBackground(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        float v = this.scroll * 0.5f;
        Screen.extractMenuBackgroundTexture(graphics, Screen.MENU_BACKGROUND, 0, 0, 0.0f, v, width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return !this.poem;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    @Override
    public void removed() {
        this.minecraft.getMusicManager().stopPlaying(Musics.CREDITS);
    }

    @Override
    public Music getBackgroundMusic() {
        return Musics.CREDITS;
    }

    @FunctionalInterface
    @Environment(value=EnvType.CLIENT)
    private static interface CreditsReader {
        public void read(Reader var1) throws IOException;
    }
}

