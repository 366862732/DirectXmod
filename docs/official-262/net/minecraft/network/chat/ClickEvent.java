/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Lifecycle
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.network.chat;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

public interface ClickEvent {
    public static final Codec<ClickEvent> CODEC = Action.CODEC.dispatch("action", ClickEvent::action, action -> action.codec);

    public Action action();

    public static enum Action implements StringRepresentable
    {
        OPEN_URL("open_url", true, OpenUrl.CODEC),
        OPEN_FILE("open_file", false, OpenFile.CODEC),
        RUN_COMMAND("run_command", true, RunCommand.CODEC),
        SUGGEST_COMMAND("suggest_command", true, SuggestCommand.CODEC),
        SHOW_DIALOG("show_dialog", true, ShowDialog.CODEC),
        CHANGE_PAGE("change_page", true, ChangePage.CODEC),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true, CopyToClipboard.CODEC),
        CUSTOM("custom", true, Custom.CODEC);

        public static final Codec<Action> UNSAFE_CODEC;
        public static final Codec<Action> CODEC;
        private final boolean allowFromServer;
        private final String name;
        private final MapCodec<? extends ClickEvent> codec;

        private Action(String name, boolean allowFromServer, MapCodec<? extends ClickEvent> codec) {
            this.name = name;
            this.allowFromServer = allowFromServer;
            this.codec = codec;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public MapCodec<? extends ClickEvent> valueCodec() {
            return this.codec;
        }

        public static DataResult<Action> filterForSerialization(Action action) {
            if (!action.isAllowedFromServer()) {
                return DataResult.error(() -> "Click event type not allowed: " + String.valueOf(action));
            }
            return DataResult.success((Object)action, (Lifecycle)Lifecycle.stable());
        }

        static {
            UNSAFE_CODEC = StringRepresentable.fromEnum(Action::values);
            CODEC = UNSAFE_CODEC.validate(Action::filterForSerialization);
        }
    }

    public record Custom(Identifier id, Optional<Tag> payload) implements ClickEvent
    {
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("id").forGetter(Custom::id), (App)ExtraCodecs.NBT.optionalFieldOf("payload").forGetter(Custom::payload)).apply((Applicative)i, Custom::new));

        @Override
        public Action action() {
            return Action.CUSTOM;
        }
    }

    public record CopyToClipboard(String value) implements ClickEvent
    {
        public static final MapCodec<CopyToClipboard> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("value").forGetter(CopyToClipboard::value)).apply((Applicative)i, CopyToClipboard::new));

        @Override
        public Action action() {
            return Action.COPY_TO_CLIPBOARD;
        }
    }

    public record ChangePage(int page) implements ClickEvent
    {
        public static final MapCodec<ChangePage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.POSITIVE_INT.fieldOf("page").forGetter(ChangePage::page)).apply((Applicative)i, ChangePage::new));

        @Override
        public Action action() {
            return Action.CHANGE_PAGE;
        }
    }

    public record ShowDialog(Holder<Dialog> dialog) implements ClickEvent
    {
        public static final MapCodec<ShowDialog> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Dialog.CODEC.fieldOf("dialog").forGetter(ShowDialog::dialog)).apply((Applicative)i, ShowDialog::new));

        @Override
        public Action action() {
            return Action.SHOW_DIALOG;
        }
    }

    public record SuggestCommand(String command) implements ClickEvent
    {
        public static final MapCodec<SuggestCommand> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(SuggestCommand::command)).apply((Applicative)i, SuggestCommand::new));

        @Override
        public Action action() {
            return Action.SUGGEST_COMMAND;
        }
    }

    public record RunCommand(String command) implements ClickEvent
    {
        public static final MapCodec<RunCommand> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.CHAT_STRING.fieldOf("command").forGetter(RunCommand::command)).apply((Applicative)i, RunCommand::new));

        @Override
        public Action action() {
            return Action.RUN_COMMAND;
        }
    }

    public record OpenFile(String path) implements ClickEvent
    {
        public static final MapCodec<OpenFile> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Codec.STRING.fieldOf("path").forGetter(OpenFile::path)).apply((Applicative)i, OpenFile::new));

        public OpenFile(File file) {
            this(file.toString());
        }

        public OpenFile(Path path) {
            this(path.toFile());
        }

        public File file() {
            return new File(this.path);
        }

        @Override
        public Action action() {
            return Action.OPEN_FILE;
        }
    }

    public record OpenUrl(URI uri) implements ClickEvent
    {
        public static final MapCodec<OpenUrl> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)ExtraCodecs.UNTRUSTED_URI.fieldOf("url").forGetter(OpenUrl::uri)).apply((Applicative)i, OpenUrl::new));

        @Override
        public Action action() {
            return Action.OPEN_URL;
        }
    }
}

