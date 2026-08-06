/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.language;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class ClientLanguage
extends Language {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, String> storage;
    private final boolean defaultRightToLeft;

    private ClientLanguage(Map<String, String> storage, boolean defaultRightToLeft) {
        this.storage = storage;
        this.defaultRightToLeft = defaultRightToLeft;
    }

    public static ClientLanguage loadFrom(ResourceManager resourceManager, List<String> languageStack, boolean defaultRightToLeft) {
        HashMap<String, String> translations = new HashMap<String, String>();
        for (String languageCode : languageStack) {
            String path = String.format(Locale.ROOT, "lang/%s.json", languageCode);
            for (String namespace : resourceManager.getNamespaces()) {
                try {
                    Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
                    ClientLanguage.appendFrom(languageCode, resourceManager.getResourceStack(location), translations);
                }
                catch (Exception e) {
                    LOGGER.warn("Skipped language file: {}:{} ({})", new Object[]{namespace, path, e.toString()});
                }
            }
        }
        DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(translations);
        return new ClientLanguage(Map.copyOf(translations), defaultRightToLeft);
    }

    private static void appendFrom(String languageCode, List<Resource> resources, Map<String, String> translations) {
        for (Resource resource : resources) {
            try {
                InputStream inputStream = resource.open();
                try {
                    Language.loadFromJson(inputStream, translations::put);
                }
                finally {
                    if (inputStream == null) continue;
                    inputStream.close();
                }
            }
            catch (IOException e) {
                LOGGER.warn("Failed to load translations for {} from pack {}", new Object[]{languageCode, resource.sourcePackId(), e});
            }
        }
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return this.storage.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean has(String key) {
        return this.storage.containsKey(key);
    }

    @Override
    public boolean isDefaultRightToLeft() {
        return this.defaultRightToLeft;
    }

    @Override
    public FormattedCharSequence getVisualOrder(FormattedText logicalOrderText) {
        return FormattedBidiReorder.reorder(logicalOrderText, this.defaultRightToLeft);
    }
}

