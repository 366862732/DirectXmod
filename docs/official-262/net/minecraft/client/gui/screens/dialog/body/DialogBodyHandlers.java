/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.gui.screens.dialog.body;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ItemDisplayWidget;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.body.DialogBodyHandler;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Style;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class DialogBodyHandlers {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<MapCodec<? extends DialogBody>, DialogBodyHandler<?>> HANDLERS = new HashMap();

    private static <B extends DialogBody> void register(MapCodec<B> type, DialogBodyHandler<? super B> handler) {
        HANDLERS.put(type, handler);
    }

    private static <B extends DialogBody> @Nullable DialogBodyHandler<B> getHandler(B body) {
        return HANDLERS.get(body.mapCodec());
    }

    public static <B extends DialogBody> @Nullable LayoutElement createBodyElement(DialogScreen<?> screen, B body) {
        DialogBodyHandler<B> handler = DialogBodyHandlers.getHandler(body);
        if (handler == null) {
            LOGGER.warn("Unrecognized dialog body {}", body);
            return null;
        }
        return handler.createControls(screen, body);
    }

    public static void bootstrap() {
        DialogBodyHandlers.register(PlainMessage.MAP_CODEC, new PlainMessageHandler());
        DialogBodyHandlers.register(ItemBody.MAP_CODEC, new ItemHandler());
    }

    private static void runActionOnParent(DialogScreen<?> parent, @Nullable Style clickedStyle) {
        ClickEvent clickEvent;
        if (clickedStyle != null && (clickEvent = clickedStyle.getClickEvent()) != null) {
            parent.runAction(Optional.of(clickEvent));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class PlainMessageHandler
    implements DialogBodyHandler<PlainMessage> {
        private PlainMessageHandler() {
        }

        @Override
        public LayoutElement createControls(DialogScreen<?> parent, PlainMessage message) {
            return FocusableTextWidget.builder(message.contents(), parent.getFont()).maxWidth(message.width()).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build().setCentered(true).setComponentClickHandler(style -> DialogBodyHandlers.runActionOnParent(parent, style));
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ItemHandler
    implements DialogBodyHandler<ItemBody> {
        private ItemHandler() {
        }

        @Override
        public LayoutElement createControls(DialogScreen<?> parent, ItemBody item) {
            ItemStack displayStack = item.item().create();
            if (item.description().isPresent()) {
                PlainMessage description = item.description().get();
                LinearLayout layout = LinearLayout.horizontal().spacing(2);
                layout.defaultCellSetting().alignVerticallyMiddle();
                ItemDisplayWidget itemWidget = new ItemDisplayWidget(Minecraft.getInstance(), 0, 0, item.width(), item.height(), CommonComponents.EMPTY, displayStack, item.showDecorations(), item.showTooltip());
                layout.addChild(itemWidget);
                layout.addChild(FocusableTextWidget.builder(description.contents(), parent.getFont()).maxWidth(description.width()).alwaysShowBorder(false).backgroundFill(FocusableTextWidget.BackgroundFill.NEVER).build().setComponentClickHandler(style -> DialogBodyHandlers.runActionOnParent(parent, style)));
                return layout;
            }
            return new ItemDisplayWidget(Minecraft.getInstance(), 0, 0, item.width(), item.height(), displayStack.getHoverName(), displayStack, item.showDecorations(), item.showTooltip());
        }
    }
}

