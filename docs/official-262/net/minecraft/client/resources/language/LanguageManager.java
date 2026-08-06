/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.metadata.language.LanguageMetadataSection;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class LanguageManager
implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LanguageInfo DEFAULT_LANGUAGE = new LanguageInfo("US", "English", false);
    private Map<String, LanguageInfo> languages = ImmutableMap.of((Object)"en_us", (Object)DEFAULT_LANGUAGE);
    private String currentCode;
    private final Consumer<ClientLanguage> reloadCallback;

    public LanguageManager(String languageCode, Consumer<ClientLanguage> reloadCallback) {
        this.currentCode = languageCode;
        this.reloadCallback = reloadCallback;
    }

    private static Map<String, LanguageInfo> extractLanguages(Stream<PackResources> resourcePacks) {
        HashMap result = Maps.newHashMap();
        resourcePacks.forEach(resourcePack -> {
            try {
                LanguageMetadataSection languageMetadataSection = resourcePack.getMetadataSection(LanguageMetadataSection.TYPE);
                if (languageMetadataSection != null) {
                    languageMetadataSection.languages().forEach(result::putIfAbsent);
                }
            }
            catch (Exception e) {
                LOGGER.warn("Unable to parse language metadata section of resourcepack: {}", (Object)resourcePack.packId(), (Object)e);
            }
        });
        return ImmutableMap.copyOf((Map)result);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        LanguageInfo currentLanguage;
        this.languages = LanguageManager.extractLanguages(resourceManager.listPacks());
        ArrayList<String> languageStack = new ArrayList<String>(2);
        boolean defaultRightToLeft = DEFAULT_LANGUAGE.bidirectional();
        languageStack.add("en_us");
        if (!this.currentCode.equals("en_us") && (currentLanguage = this.languages.get(this.currentCode)) != null) {
            languageStack.add(this.currentCode);
            defaultRightToLeft = currentLanguage.bidirectional();
        }
        ClientLanguage locale = ClientLanguage.loadFrom(resourceManager, languageStack, defaultRightToLeft);
        Language.inject(locale);
        this.reloadCallback.accept(locale);
    }

    public void setSelected(String code) {
        this.currentCode = code;
    }

    public String getSelected() {
        return this.currentCode;
    }

    public SortedMap<String, LanguageInfo> getLanguages() {
        return new TreeMap<String, LanguageInfo>(this.languages);
    }

    public @Nullable LanguageInfo getLanguage(String code) {
        return this.languages.get(code);
    }
}

