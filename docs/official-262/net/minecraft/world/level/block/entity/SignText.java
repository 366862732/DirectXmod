/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class SignText {
    private static final Codec<Component[]> LINES_CODEC = ComponentSerialization.CODEC.listOf().comapFlatMap(input -> {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn.rewriteExpressions(StructuredReturn.java:99)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1050)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }, components -> List.of(components[0], components[1], components[2], components[3]));
    public static final Codec<SignText> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group((App)LINES_CODEC.fieldOf("messages").forGetter(o -> o.messages), (App)LINES_CODEC.lenientOptionalFieldOf("filtered_messages").forGetter(SignText::filteredMessages), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(DyeColor.CODEC, "color", DyeColor.BLACK).forGetter(o -> o.color), (App)ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "has_glowing_text", false).forGetter(o -> o.hasGlowingText)).apply((Applicative)i, SignText::load));
    public static final int LINES = 4;
    private final Component[] messages;
    private final Component[] filteredMessages;
    private final DyeColor color;
    private final boolean hasGlowingText;
    private FormattedCharSequence @Nullable [] renderMessages;
    private boolean renderMessagedFiltered;

    public SignText() {
        this(SignText.emptyMessages(), SignText.emptyMessages(), DyeColor.BLACK, false);
    }

    public SignText(Component[] messages, Component[] filteredMessages, DyeColor color, boolean hasGlowingText) {
        this.messages = messages;
        this.filteredMessages = filteredMessages;
        this.color = color;
        this.hasGlowingText = hasGlowingText;
    }

    private static Component[] emptyMessages() {
        return new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY};
    }

    private static SignText load(Component[] messages, Optional<Component[]> filteredMessages, DyeColor color, boolean hasGlowingText) {
        return new SignText(messages, filteredMessages.orElse(Arrays.copyOf(messages, messages.length)), color, hasGlowingText);
    }

    public boolean hasGlowingText() {
        return this.hasGlowingText;
    }

    public SignText setHasGlowingText(boolean hasGlowingText) {
        if (hasGlowingText == this.hasGlowingText) {
            return this;
        }
        return new SignText(this.messages, this.filteredMessages, this.color, hasGlowingText);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public SignText setColor(DyeColor color) {
        if (color == this.getColor()) {
            return this;
        }
        return new SignText(this.messages, this.filteredMessages, color, this.hasGlowingText);
    }

    public Component getMessage(int index, boolean shouldFilter) {
        return this.getMessages(shouldFilter)[index];
    }

    public SignText setMessage(int index, Component message) {
        return this.setMessage(index, message, message);
    }

    public SignText setMessage(int index, Component rawMessage, Component filteredMessage) {
        Component[] messages = Arrays.copyOf(this.messages, this.messages.length);
        Component[] filteredMessages = Arrays.copyOf(this.filteredMessages, this.filteredMessages.length);
        messages[index] = rawMessage;
        filteredMessages[index] = filteredMessage;
        return new SignText(messages, filteredMessages, this.color, this.hasGlowingText);
    }

    public boolean hasMessage(Player player) {
        return Arrays.stream(this.getMessages(player.isTextFilteringEnabled())).anyMatch(component -> !component.getString().isEmpty());
    }

    public Component[] getMessages(boolean shouldFilter) {
        return shouldFilter ? this.filteredMessages : this.messages;
    }

    public FormattedCharSequence[] getRenderMessages(boolean shouldFilter, Function<Component, FormattedCharSequence> prepare) {
        if (this.renderMessages == null || this.renderMessagedFiltered != shouldFilter) {
            this.renderMessagedFiltered = shouldFilter;
            this.renderMessages = new FormattedCharSequence[4];
            for (int i = 0; i < 4; ++i) {
                this.renderMessages[i] = prepare.apply(this.getMessage(i, shouldFilter));
            }
        }
        return this.renderMessages;
    }

    private Optional<Component[]> filteredMessages() {
        for (int i = 0; i < 4; ++i) {
            if (this.filteredMessages[i].equals(this.messages[i])) continue;
            return Optional.of(this.filteredMessages);
        }
        return Optional.empty();
    }

    public boolean hasAnyClickCommands(Player player) {
        for (Component message : this.getMessages(player.isTextFilteringEnabled())) {
            Style style = message.getStyle();
            ClickEvent event = style.getClickEvent();
            if (event == null || event.action() != ClickEvent.Action.RUN_COMMAND) continue;
            return true;
        }
        return false;
    }
}

