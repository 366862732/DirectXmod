package com.xgdt.dx12;

import com.xgdt.dx12.gui.Dx12SettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu integration: exposes our Dx12SettingsScreen as the mod's config screen.
 * ModMenu automatically adds a "Config" button on our mod's entry in the mod list.
 * No OptionsScreen mixin needed.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Dx12SettingsScreen::new;
    }
}
