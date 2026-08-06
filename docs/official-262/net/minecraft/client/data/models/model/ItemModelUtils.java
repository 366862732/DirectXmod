/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.model;

import com.mojang.math.Transformation;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;

@Environment(value=EnvType.CLIENT)
public class ItemModelUtils {
    public static ItemModel.Unbaked plainModel(Identifier id) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of());
    }

    public static ItemModel.Unbaked plainModel(Identifier id, Transformation transformation) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.of(transformation), List.of());
    }

    public static ItemModel.Unbaked tintedModel(Identifier id, ItemTintSource ... tints) {
        return new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), List.of(tints));
    }

    public static ItemTintSource constantTint(int color) {
        return new Constant(color);
    }

    public static ItemModel.Unbaked composite(ItemModel.Unbaked ... models) {
        return new CompositeModel.Unbaked(List.of(models), Optional.empty());
    }

    public static ItemModel.Unbaked specialModel(Identifier base, SpecialModelRenderer.Unbaked<?> model) {
        return ItemModelUtils.specialModel(base, Optional.empty(), model);
    }

    public static ItemModel.Unbaked specialModel(Identifier base, Transformation transformation, SpecialModelRenderer.Unbaked<?> model) {
        return ItemModelUtils.specialModel(base, Optional.of(transformation), model);
    }

    public static ItemModel.Unbaked specialModel(Identifier base, Optional<Transformation> transformation, SpecialModelRenderer.Unbaked<?> model) {
        return new SpecialModelWrapper.Unbaked(base, transformation, model);
    }

    public static RangeSelectItemModel.Entry override(ItemModel.Unbaked model, float value) {
        return new RangeSelectItemModel.Entry(value, model);
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, ItemModel.Unbaked fallback, RangeSelectItemModel.Entry ... entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0f, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, float scale, ItemModel.Unbaked fallback, RangeSelectItemModel.Entry ... entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, scale, List.of(entries), Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, ItemModel.Unbaked fallback, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0f, entries, Optional.of(fallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, 1.0f, entries, Optional.empty());
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries) {
        return new RangeSelectItemModel.Unbaked(Optional.empty(), property, scale, entries, Optional.empty());
    }

    public static ItemModel.Unbaked conditional(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) {
        return ItemModelUtils.conditional(Optional.empty(), property, onTrue, onFalse);
    }

    public static ItemModel.Unbaked conditional(Transformation transformation, ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) {
        return ItemModelUtils.conditional(Optional.of(transformation), property, onTrue, onFalse);
    }

    public static ItemModel.Unbaked conditional(Optional<Transformation> transformation, ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) {
        return new ConditionalItemModel.Unbaked(transformation, property, onTrue, onFalse);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(T value, ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<T>(List.of(value), model);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(List<T> values, ItemModel.Unbaked model) {
        return new SelectItemModel.SwitchCase<T>(values, model);
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, ItemModel.Unbaked fallback, SelectItemModel.SwitchCase<T> ... cases) {
        return ItemModelUtils.select(property, fallback, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, ItemModel.Unbaked fallback, List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(Optional.empty(), new SelectItemModel.UnbakedSwitch<SelectItemModelProperty<T>, T>(property, cases), Optional.of(fallback));
    }

    public static <T> ItemModel.Unbaked select(Transformation transformation, SelectItemModelProperty<T> property, ItemModel.Unbaked fallback, List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(Optional.of(transformation), new SelectItemModel.UnbakedSwitch<SelectItemModelProperty<T>, T>(property, cases), Optional.of(fallback));
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, SelectItemModel.SwitchCase<T> ... cases) {
        return ItemModelUtils.select(property, List.of(cases));
    }

    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> property, List<SelectItemModel.SwitchCase<T>> cases) {
        return new SelectItemModel.Unbaked(Optional.empty(), new SelectItemModel.UnbakedSwitch<SelectItemModelProperty<T>, T>(property, cases), Optional.empty());
    }

    public static ConditionalItemModelProperty isUsingItem() {
        return new IsUsingItem();
    }

    public static ConditionalItemModelProperty hasComponent(DataComponentType<?> component) {
        return new HasComponent(component, false);
    }

    public static ItemModel.Unbaked inOverworld(ItemModel.Unbaked ifTrue, ItemModel.Unbaked ifFalse) {
        return ItemModelUtils.select(new ContextDimension(), ifFalse, ItemModelUtils.when(Level.OVERWORLD, ifTrue));
    }

    private static <T extends Comparable<T>> List<SelectItemModel.SwitchCase<String>> createBlockPropertySwitchCases(Property<T> property, Map<T, ItemModel.Unbaked> cases) {
        return cases.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> {
            String valueName = property.getName((Comparable)e.getKey());
            return new SelectItemModel.SwitchCase<String>(List.of(valueName), (ItemModel.Unbaked)e.getValue());
        }).toList();
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(Property<T> property, ItemModel.Unbaked fallback, Map<T, ItemModel.Unbaked> cases) {
        return ItemModelUtils.select(new ItemBlockState(property.getName()), fallback, ItemModelUtils.createBlockPropertySwitchCases(property, cases));
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(Transformation transformation, Property<T> property, ItemModel.Unbaked fallback, Map<T, ItemModel.Unbaked> cases) {
        return ItemModelUtils.select(transformation, new ItemBlockState(property.getName()), fallback, ItemModelUtils.createBlockPropertySwitchCases(property, cases));
    }

    public static ItemModel.Unbaked isXmas(ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd", Locale.ROOT);
        List<String> days = SpecialDates.CHRISTMAS_RANGE.stream().map(formatter::format).toList();
        return ItemModelUtils.select(LocalTime.create("MM-dd", "", Optional.empty()), onFalse, List.of(ItemModelUtils.when(days, onTrue)));
    }
}

