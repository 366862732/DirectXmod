/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.MonthDay;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpecialDates;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class SplashManager
extends SimplePreparableReloadListener<List<Component>> {
    private static final Style DEFAULT_STYLE = Style.EMPTY.withColor(-256);
    public static final Component CHRISTMAS = SplashManager.literalSplash("Merry X-mas!");
    public static final Component NEW_YEAR = SplashManager.literalSplash("Happy new year!");
    public static final Component HALLOWEEN = SplashManager.literalSplash("OOoooOOOoooo! Spooky!");
    private static final Identifier SPLASHES_LOCATION = Identifier.withDefaultNamespace("texts/splashes.txt");
    private static final RandomSource RANDOM = RandomSource.create();
    private List<Component> splashes = List.of();
    private final User user;

    public SplashManager(User user) {
        this.user = user;
    }

    private static Component literalSplash(String text) {
        return Component.literal(text).setStyle(DEFAULT_STYLE);
    }

    @Override
    protected List<Component> prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<Component> list;
        block8: {
            BufferedReader reader = Minecraft.getInstance().getResourceManager().openAsReader(SPLASHES_LOCATION);
            try {
                list = reader.lines().map(String::trim).filter(line -> line.hashCode() != 125780783).map(SplashManager::literalSplash).toList();
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            reader.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException ignored) {
                    return List.of();
                }
            }
            reader.close();
        }
        return list;
    }

    @Override
    protected void apply(List<Component> preparations, ResourceManager manager, ProfilerFiller profiler) {
        this.splashes = List.copyOf(preparations);
    }

    public @Nullable SplashRenderer getSplash() {
        MonthDay monthDay = SpecialDates.dayNow();
        if (monthDay.equals(SpecialDates.CHRISTMAS)) {
            return SplashRenderer.CHRISTMAS;
        }
        if (monthDay.equals(SpecialDates.NEW_YEAR)) {
            return SplashRenderer.NEW_YEAR;
        }
        if (monthDay.equals(SpecialDates.HALLOWEEN)) {
            return SplashRenderer.HALLOWEEN;
        }
        if (this.splashes.isEmpty()) {
            return null;
        }
        if (this.user != null && RANDOM.nextInt(this.splashes.size()) == 42) {
            return new SplashRenderer(SplashManager.literalSplash(this.user.getName().toUpperCase(Locale.ROOT) + " IS YOU"));
        }
        return new SplashRenderer(this.splashes.get(RANDOM.nextInt(this.splashes.size())));
    }
}

