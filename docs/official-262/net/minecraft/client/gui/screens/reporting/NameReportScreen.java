/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.AbstractReportScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class NameReportScreen
extends AbstractReportScreen<NameReport.Builder> {
    private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
    private static final Component COMMENT_BOX_LABEL = Component.translatable("gui.abuseReport.name.comment_box_label");
    private @Nullable MultiLineEditBox commentBox;

    private NameReportScreen(Screen lastScreen, ReportingContext reportingContext, NameReport.Builder reportBuilder) {
        super(TITLE, lastScreen, reportingContext, reportBuilder);
    }

    public NameReportScreen(Screen lastScreen, ReportingContext reportingContext, UUID playerId, String reportedName) {
        this(lastScreen, reportingContext, new NameReport.Builder(playerId, reportedName, reportingContext.sender().reportLimits()));
    }

    public NameReportScreen(Screen lastScreen, ReportingContext reportingContext, NameReport draft) {
        this(lastScreen, reportingContext, new NameReport.Builder(draft, reportingContext.sender().reportLimits()));
    }

    @Override
    protected void addContent() {
        MutableComponent reportedName = Component.literal(((NameReport)((NameReport.Builder)this.reportBuilder).report()).getReportedName()).withStyle(ChatFormatting.YELLOW);
        this.layout.addChild(new StringWidget(Component.translatable("gui.abuseReport.name.reporting", reportedName), this.font), s -> s.alignHorizontallyCenter().padding(0, 8));
        this.commentBox = this.createCommentBox(280, this.font.lineHeight * 8, comments -> {
            ((NameReport.Builder)this.reportBuilder).setComments((String)comments);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, COMMENT_BOX_LABEL, s -> s.paddingBottom(12)));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (super.mouseReleased(event)) {
            return true;
        }
        if (this.commentBox != null) {
            return this.commentBox.mouseReleased(event);
        }
        return false;
    }
}

