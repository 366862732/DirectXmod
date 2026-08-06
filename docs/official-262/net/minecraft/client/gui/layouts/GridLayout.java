/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

@Environment(value=EnvType.CLIENT)
public class GridLayout
extends AbstractLayout {
    private final List<ChildContainer> children = new ArrayList<ChildContainer>();
    private final LayoutSettings defaultCellSettings = LayoutSettings.defaults();
    private int rowSpacing = 0;
    private int columnSpacing = 0;

    public GridLayout() {
        this(0, 0);
    }

    public GridLayout(int x, int y) {
        super(x, y, 0, 0);
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();
        int maxRow = 0;
        int maxColumn = 0;
        for (ChildContainer child : this.children) {
            maxRow = Math.max(child.getLastOccupiedRow(), maxRow);
            maxColumn = Math.max(child.getLastOccupiedColumn(), maxColumn);
        }
        int[] maxColumnWidths = new int[maxColumn + 1];
        int[] maxRowHeights = new int[maxRow + 1];
        for (ChildContainer child : this.children) {
            int childHeight = child.getHeight() - (child.occupiedRows - 1) * this.rowSpacing;
            Divisor heightDivisor = new Divisor(childHeight, child.occupiedRows);
            for (int row = child.row; row <= child.getLastOccupiedRow(); ++row) {
                maxRowHeights[row] = Math.max(maxRowHeights[row], heightDivisor.nextInt());
            }
            int childWidth = child.getWidth() - (child.occupiedColumns - 1) * this.columnSpacing;
            Divisor widthDivisor = new Divisor(childWidth, child.occupiedColumns);
            for (int column = child.column; column <= child.getLastOccupiedColumn(); ++column) {
                maxColumnWidths[column] = Math.max(maxColumnWidths[column], widthDivisor.nextInt());
            }
        }
        int[] columnXOffsets = new int[maxColumn + 1];
        int[] rowYOffsets = new int[maxRow + 1];
        columnXOffsets[0] = 0;
        for (int column = 1; column <= maxColumn; ++column) {
            columnXOffsets[column] = columnXOffsets[column - 1] + maxColumnWidths[column - 1] + this.columnSpacing;
        }
        rowYOffsets[0] = 0;
        for (int row = 1; row <= maxRow; ++row) {
            rowYOffsets[row] = rowYOffsets[row - 1] + maxRowHeights[row - 1] + this.rowSpacing;
        }
        for (ChildContainer child : this.children) {
            int availableWidth = 0;
            for (int column = child.column; column <= child.getLastOccupiedColumn(); ++column) {
                availableWidth += maxColumnWidths[column];
            }
            child.setX(this.getX() + columnXOffsets[child.column], availableWidth += this.columnSpacing * (child.occupiedColumns - 1));
            int availableHeight = 0;
            for (int row = child.row; row <= child.getLastOccupiedRow(); ++row) {
                availableHeight += maxRowHeights[row];
            }
            child.setY(this.getY() + rowYOffsets[child.row], availableHeight += this.rowSpacing * (child.occupiedRows - 1));
        }
        this.width = columnXOffsets[maxColumn] + maxColumnWidths[maxColumn];
        this.height = rowYOffsets[maxRow] + maxRowHeights[maxRow];
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column) {
        return this.addChild(child, row, column, this.newCellSettings());
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column, LayoutSettings cellSettings) {
        return this.addChild(child, row, column, 1, 1, cellSettings);
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.addChild(child, row, column, 1, 1, Util.make(this.newCellSettings(), layoutSettingsAdjustments));
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column, int rows, int columns) {
        return this.addChild(child, row, column, rows, columns, this.newCellSettings());
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column, int rows, int columns, LayoutSettings cellSettings) {
        if (rows < 1) {
            throw new IllegalArgumentException("Occupied rows must be at least 1");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("Occupied columns must be at least 1");
        }
        this.children.add(new ChildContainer(child, row, column, rows, columns, cellSettings));
        return child;
    }

    public <T extends LayoutElement> T addChild(T child, int row, int column, int rows, int columns, Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.addChild(child, row, column, rows, columns, Util.make(this.newCellSettings(), layoutSettingsAdjustments));
    }

    public GridLayout columnSpacing(int columnSpacing) {
        this.columnSpacing = columnSpacing;
        return this;
    }

    public GridLayout rowSpacing(int rowSpacing) {
        this.rowSpacing = rowSpacing;
        return this;
    }

    public GridLayout spacing(int spacing) {
        return this.columnSpacing(spacing).rowSpacing(spacing);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        this.children.forEach(child -> layoutElementVisitor.accept(child.child));
    }

    @Override
    public void removeChildren() {
        this.children.clear();
    }

    public LayoutSettings newCellSettings() {
        return this.defaultCellSettings.copy();
    }

    public LayoutSettings defaultCellSetting() {
        return this.defaultCellSettings;
    }

    public RowHelper createRowHelper(int columns) {
        return new RowHelper(this, columns);
    }

    @Environment(value=EnvType.CLIENT)
    private static class ChildContainer
    extends AbstractLayout.AbstractChildWrapper {
        private final int row;
        private final int column;
        private final int occupiedRows;
        private final int occupiedColumns;

        private ChildContainer(LayoutElement widget, int row, int column, int occupiedRows, int occupiedColumns, LayoutSettings cellSettings) {
            super(widget, cellSettings.getExposed());
            this.row = row;
            this.column = column;
            this.occupiedRows = occupiedRows;
            this.occupiedColumns = occupiedColumns;
        }

        public int getLastOccupiedRow() {
            return this.row + this.occupiedRows - 1;
        }

        public int getLastOccupiedColumn() {
            return this.column + this.occupiedColumns - 1;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public final class RowHelper {
        private final int columns;
        private int index;
        final /* synthetic */ GridLayout this$0;

        private RowHelper(GridLayout this$0, int columns) {
            GridLayout gridLayout = this$0;
            Objects.requireNonNull(gridLayout);
            this.this$0 = gridLayout;
            this.columns = columns;
        }

        public <T extends LayoutElement> T addChild(T widget) {
            return this.addChild(widget, 1);
        }

        public <T extends LayoutElement> T addChild(T widget, int columnWidth) {
            return this.addChild(widget, columnWidth, this.defaultCellSetting());
        }

        public <T extends LayoutElement> T addChild(T widget, LayoutSettings layoutSettings) {
            return this.addChild(widget, 1, layoutSettings);
        }

        public <T extends LayoutElement> T addChild(T widget, int columnWidth, LayoutSettings layoutSettings) {
            int row = this.index / this.columns;
            int columnBegin = this.index % this.columns;
            if (columnBegin + columnWidth > this.columns) {
                ++row;
                columnBegin = 0;
                this.index = Mth.roundToward(this.index, this.columns);
            }
            this.index += columnWidth;
            return this.this$0.addChild(widget, row, columnBegin, 1, columnWidth, layoutSettings);
        }

        public GridLayout getGrid() {
            return this.this$0;
        }

        public LayoutSettings newCellSettings() {
            return this.this$0.newCellSettings();
        }

        public LayoutSettings defaultCellSetting() {
            return this.this$0.defaultCellSetting();
        }
    }
}

