/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class KeyMapping
implements Comparable<KeyMapping> {
    private static final Map<String, KeyMapping> ALL = Maps.newHashMap();
    private static final Map<InputConstants.Key, List<KeyMapping>> MAP = Maps.newHashMap();
    private final String name;
    private final InputConstants.Key defaultKey;
    private final Category category;
    protected InputConstants.Key key;
    private boolean isDown;
    private int clickCount;
    private final int order;

    public static void click(InputConstants.Key key) {
        KeyMapping.forAllKeyMappings(key, keyMapping -> ++keyMapping.clickCount);
    }

    public static void set(InputConstants.Key key, boolean state) {
        KeyMapping.forAllKeyMappings(key, keyMapping -> keyMapping.setDown(state));
    }

    private static void forAllKeyMappings(InputConstants.Key key, Consumer<KeyMapping> operation) {
        List<KeyMapping> keyMappings = MAP.get(key);
        if (keyMappings != null && !keyMappings.isEmpty()) {
            for (KeyMapping keyMapping : keyMappings) {
                operation.accept(keyMapping);
            }
        }
    }

    public static void setAll() {
        Window window = Minecraft.getInstance().getWindow();
        for (KeyMapping keyMapping : ALL.values()) {
            if (!keyMapping.shouldSetOnIngameFocus()) continue;
            keyMapping.setDown(InputConstants.isKeyDown(window, keyMapping.key.getValue()));
        }
    }

    public static void releaseAll() {
        for (KeyMapping keyMapping : ALL.values()) {
            keyMapping.release();
        }
    }

    public static void restoreToggleStatesOnScreenClosed() {
        for (KeyMapping keyMapping : ALL.values()) {
            ToggleKeyMapping toggleKeyMapping;
            if (!(keyMapping instanceof ToggleKeyMapping) || !(toggleKeyMapping = (ToggleKeyMapping)keyMapping).shouldRestoreStateOnScreenClosed()) continue;
            toggleKeyMapping.setDown(true);
        }
    }

    public static void resetToggleKeys() {
        for (KeyMapping keyMapping : ALL.values()) {
            if (!(keyMapping instanceof ToggleKeyMapping)) continue;
            ToggleKeyMapping toggleKeyMapping = (ToggleKeyMapping)keyMapping;
            toggleKeyMapping.reset();
        }
    }

    public static void resetMapping() {
        MAP.clear();
        for (KeyMapping keyMapping : ALL.values()) {
            keyMapping.registerMapping(keyMapping.key);
        }
    }

    public KeyMapping(String name, int keysym, Category category) {
        this(name, InputConstants.Type.KEYSYM, keysym, category);
    }

    public KeyMapping(String name, InputConstants.Type type, int value, Category category) {
        this(name, type, value, category, 0);
    }

    public KeyMapping(String name, InputConstants.Type type, int value, Category category, int order) {
        this.name = name;
        this.defaultKey = this.key = type.getOrCreate(value);
        this.category = category;
        this.order = order;
        ALL.put(name, this);
        this.registerMapping(this.key);
    }

    public boolean isDown() {
        return this.isDown;
    }

    public Category getCategory() {
        return this.category;
    }

    public boolean consumeClick() {
        if (this.clickCount == 0) {
            return false;
        }
        --this.clickCount;
        return true;
    }

    protected void release() {
        this.clickCount = 0;
        this.setDown(false);
    }

    protected boolean shouldSetOnIngameFocus() {
        return this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() != InputConstants.UNKNOWN.getValue();
    }

    public String getName() {
        return this.name;
    }

    public InputConstants.Key getDefaultKey() {
        return this.defaultKey;
    }

    public void setKey(InputConstants.Key key) {
        this.key = key;
    }

    @Override
    public int compareTo(KeyMapping o) {
        if (this.category == o.category) {
            if (this.order == o.order) {
                return I18n.get(this.name, new Object[0]).compareTo(I18n.get(o.name, new Object[0]));
            }
            return Integer.compare(this.order, o.order);
        }
        return Integer.compare(Category.SORT_ORDER.indexOf(this.category), Category.SORT_ORDER.indexOf(o.category));
    }

    public static Supplier<Component> createNameSupplier(String key) {
        KeyMapping map = ALL.get(key);
        if (map == null) {
            return () -> Component.translatable(key);
        }
        return map::getTranslatedKeyMessage;
    }

    public boolean same(KeyMapping that) {
        return this.key.equals(that.key);
    }

    public boolean isUnbound() {
        return this.key.equals(InputConstants.UNKNOWN);
    }

    public boolean matches(KeyEvent event) {
        if (event.key() == InputConstants.UNKNOWN.getValue()) {
            return this.key.getType() == InputConstants.Type.SCANCODE && this.key.getValue() == event.scancode();
        }
        return this.key.getType() == InputConstants.Type.KEYSYM && this.key.getValue() == event.key();
    }

    public boolean matchesMouse(MouseButtonEvent event) {
        return this.key.getType() == InputConstants.Type.MOUSE && this.key.getValue() == event.button();
    }

    public boolean matches(InputConstants.Key key) {
        return this.key.equals(key);
    }

    public Component getTranslatedKeyMessage() {
        return this.key.getDisplayName();
    }

    public boolean isDefault() {
        return this.key.equals(this.defaultKey);
    }

    public String saveString() {
        return this.key.getName();
    }

    public void setDown(boolean down) {
        this.isDown = down;
    }

    private void registerMapping(InputConstants.Key key) {
        MAP.computeIfAbsent(key, k -> new ArrayList()).add(this);
    }

    public static @Nullable KeyMapping get(String name) {
        return ALL.get(name);
    }

    @Environment(value=EnvType.CLIENT)
    public record Category(Identifier id) {
        private static final List<Category> SORT_ORDER = new ArrayList<Category>();
        public static final Category MOVEMENT = Category.register("movement");
        public static final Category MISC = Category.register("misc");
        public static final Category MULTIPLAYER = Category.register("multiplayer");
        public static final Category GAMEPLAY = Category.register("gameplay");
        public static final Category INVENTORY = Category.register("inventory");
        public static final Category CREATIVE = Category.register("creative");
        public static final Category SPECTATOR = Category.register("spectator");
        public static final Category DEBUG = Category.register("debug");

        private static Category register(String name) {
            return Category.register(Identifier.withDefaultNamespace(name));
        }

        public static Category register(Identifier id) {
            Category category = new Category(id);
            if (SORT_ORDER.contains(category)) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", id));
            }
            SORT_ORDER.add(category);
            return category;
        }

        public Component label() {
            return Component.translatable(this.id.toLanguageKey("key.category"));
        }
    }
}

