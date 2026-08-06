/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.mojang.logging.LogUtils
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class MenuScreens {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MenuType<?>, ScreenConstructor<?, ?>> SCREENS = Maps.newHashMap();

    public static <T extends AbstractContainerMenu> void create(MenuType<T> type, Minecraft minecraft, int containerId, Component title) {
        ScreenConstructor<T, ?> constructor = MenuScreens.getConstructor(type);
        if (constructor == null) {
            LOGGER.warn("Failed to create screen for menu type: {}", (Object)BuiltInRegistries.MENU.getKey(type));
            return;
        }
        constructor.fromPacket(title, type, minecraft, containerId);
    }

    private static <T extends AbstractContainerMenu> @Nullable ScreenConstructor<T, ?> getConstructor(MenuType<T> type) {
        return SCREENS.get(type);
    }

    private static <M extends AbstractContainerMenu, U extends Screen> void register(MenuType<? extends M> type, ScreenConstructor<M, U> factory) {
        ScreenConstructor<M, U> prev = SCREENS.put(type, factory);
        if (prev != null) {
            throw new IllegalStateException("Duplicate registration for " + String.valueOf(BuiltInRegistries.MENU.getKey(type)));
        }
    }

    public static boolean selfTest() {
        boolean failed = false;
        for (MenuType menuType : BuiltInRegistries.MENU) {
            if (SCREENS.containsKey(menuType)) continue;
            LOGGER.debug("Menu {} has no matching screen", (Object)BuiltInRegistries.MENU.getKey(menuType));
            failed = true;
        }
        return failed;
    }

    static {
        MenuScreens.register(MenuType.GENERIC_9x1, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_9x2, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_9x3, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_9x4, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_9x5, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_9x6, ContainerScreen::new);
        MenuScreens.register(MenuType.GENERIC_3x3, DispenserScreen::new);
        MenuScreens.register(MenuType.CRAFTER_3x3, CrafterScreen::new);
        MenuScreens.register(MenuType.ANVIL, AnvilScreen::new);
        MenuScreens.register(MenuType.BEACON, BeaconScreen::new);
        MenuScreens.register(MenuType.BLAST_FURNACE, BlastFurnaceScreen::new);
        MenuScreens.register(MenuType.BREWING_STAND, BrewingStandScreen::new);
        MenuScreens.register(MenuType.CRAFTING, CraftingScreen::new);
        MenuScreens.register(MenuType.ENCHANTMENT, EnchantmentScreen::new);
        MenuScreens.register(MenuType.FURNACE, FurnaceScreen::new);
        MenuScreens.register(MenuType.GRINDSTONE, GrindstoneScreen::new);
        MenuScreens.register(MenuType.HOPPER, HopperScreen::new);
        MenuScreens.register(MenuType.LECTERN, LecternScreen::new);
        MenuScreens.register(MenuType.LOOM, LoomScreen::new);
        MenuScreens.register(MenuType.MERCHANT, MerchantScreen::new);
        MenuScreens.register(MenuType.SHULKER_BOX, ShulkerBoxScreen::new);
        MenuScreens.register(MenuType.SMITHING, SmithingScreen::new);
        MenuScreens.register(MenuType.SMOKER, SmokerScreen::new);
        MenuScreens.register(MenuType.CARTOGRAPHY_TABLE, CartographyTableScreen::new);
        MenuScreens.register(MenuType.STONECUTTER, StonecutterScreen::new);
    }

    @Environment(value=EnvType.CLIENT)
    private static interface ScreenConstructor<T extends AbstractContainerMenu, U extends Screen> {
        default public void fromPacket(Component title, MenuType<T> type, Minecraft minecraft, int containerId) {
            U screen = this.create(type.create(containerId, minecraft.player.getInventory()), minecraft.player.getInventory(), title);
            minecraft.player.containerMenu = ((MenuAccess)screen).getMenu();
            minecraft.gui.setScreen((Screen)screen);
        }

        public U create(T var1, Inventory var2, Component var3);
    }
}

