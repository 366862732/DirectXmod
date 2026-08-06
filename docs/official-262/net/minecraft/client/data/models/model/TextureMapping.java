/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.data.models.model;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;

@Environment(value=EnvType.CLIENT)
public class TextureMapping {
    private final Map<TextureSlot, Material> slots = Maps.newHashMap();
    private final Set<TextureSlot> forcedSlots = Sets.newHashSet();

    public TextureMapping put(TextureSlot slot, Material material) {
        this.slots.put(slot, material);
        return this;
    }

    public TextureMapping putForced(TextureSlot slot, Material material) {
        this.slots.put(slot, material);
        this.forcedSlots.add(slot);
        return this;
    }

    public Stream<TextureSlot> getForced() {
        return this.forcedSlots.stream();
    }

    public TextureMapping copySlot(TextureSlot from, TextureSlot to) {
        return this.put(to, this.slots.get(from));
    }

    public TextureMapping copyForced(TextureSlot from, TextureSlot to) {
        return this.putForced(to, this.slots.get(from));
    }

    public Material get(TextureSlot slot) {
        for (TextureSlot currentSlot = slot; currentSlot != null; currentSlot = currentSlot.getParent()) {
            Material result = this.slots.get(currentSlot);
            if (result == null) continue;
            return result;
        }
        throw new IllegalStateException("Can't find texture for slot " + String.valueOf(slot));
    }

    public TextureMapping copyAndUpdate(TextureSlot slot, Material material) {
        TextureMapping result = new TextureMapping();
        result.slots.putAll(this.slots);
        result.forcedSlots.addAll(this.forcedSlots);
        result.put(slot, material);
        return result;
    }

    public TextureMapping updateSlots(BiFunction<TextureSlot, Material, Material> mapper) {
        this.slots.replaceAll(mapper);
        return this;
    }

    public TextureMapping forceAllTranslucent() {
        return this.updateSlots((textureSlot, material) -> material.withForceTranslucent(true));
    }

    public static TextureMapping cube(Block block) {
        Material texture = TextureMapping.getBlockTexture(block);
        return TextureMapping.cube(texture);
    }

    public static TextureMapping defaultTexture(Block block) {
        Material texture = TextureMapping.getBlockTexture(block);
        return TextureMapping.defaultTexture(texture);
    }

    public static TextureMapping defaultTexture(Material texture) {
        return new TextureMapping().put(TextureSlot.TEXTURE, texture);
    }

    public static TextureMapping cube(Material all) {
        return new TextureMapping().put(TextureSlot.ALL, all);
    }

    public static TextureMapping cross(Block block) {
        return TextureMapping.singleSlot(TextureSlot.CROSS, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping side(Block block) {
        return TextureMapping.singleSlot(TextureSlot.SIDE, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping crossEmissive(Block block) {
        return new TextureMapping().put(TextureSlot.CROSS, TextureMapping.getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, TextureMapping.getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping cross(Material cross) {
        return TextureMapping.singleSlot(TextureSlot.CROSS, cross);
    }

    public static TextureMapping plant(Block block) {
        return TextureMapping.singleSlot(TextureSlot.PLANT, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping plantEmissive(Block block) {
        return new TextureMapping().put(TextureSlot.PLANT, TextureMapping.getBlockTexture(block)).put(TextureSlot.CROSS_EMISSIVE, TextureMapping.getBlockTexture(block, "_emissive"));
    }

    public static TextureMapping plant(Material plant) {
        return TextureMapping.singleSlot(TextureSlot.PLANT, plant);
    }

    public static TextureMapping rail(Block block) {
        return TextureMapping.singleSlot(TextureSlot.RAIL, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping rail(Material rail) {
        return TextureMapping.singleSlot(TextureSlot.RAIL, rail);
    }

    public static TextureMapping wool(Block block) {
        return TextureMapping.singleSlot(TextureSlot.WOOL, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping flowerbed(Block block) {
        return new TextureMapping().put(TextureSlot.FLOWERBED, TextureMapping.getBlockTexture(block)).put(TextureSlot.STEM, TextureMapping.getBlockTexture(block, "_stem"));
    }

    public static TextureMapping wool(Material cross) {
        return TextureMapping.singleSlot(TextureSlot.WOOL, cross);
    }

    public static TextureMapping stem(Block block) {
        return TextureMapping.singleSlot(TextureSlot.STEM, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping attachedStem(Block stem, Block upperStem) {
        return new TextureMapping().put(TextureSlot.STEM, TextureMapping.getBlockTexture(stem)).put(TextureSlot.UPPER_STEM, TextureMapping.getBlockTexture(upperStem));
    }

    public static TextureMapping pattern(Block block) {
        return TextureMapping.singleSlot(TextureSlot.PATTERN, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping fan(Block block) {
        return TextureMapping.singleSlot(TextureSlot.FAN, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping crop(Material id) {
        return TextureMapping.singleSlot(TextureSlot.CROP, id);
    }

    public static TextureMapping pane(Block body, Block edge) {
        return new TextureMapping().put(TextureSlot.PANE, TextureMapping.getBlockTexture(body)).put(TextureSlot.EDGE, TextureMapping.getBlockTexture(edge, "_top"));
    }

    public static TextureMapping singleSlot(TextureSlot slot, Material id) {
        return new TextureMapping().put(slot, id);
    }

    public static TextureMapping column(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping cubeTop(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping pottedAzalea(Block block) {
        return new TextureMapping().put(TextureSlot.PLANT, TextureMapping.getBlockTexture(block, "_plant")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping logColumn(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block)).put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_top")).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping column(Material side, Material end) {
        return new TextureMapping().put(TextureSlot.SIDE, side).put(TextureSlot.END, end);
    }

    public static TextureMapping fence(Block block) {
        return new TextureMapping().put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(block)).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping customParticle(Block block) {
        return new TextureMapping().put(TextureSlot.TEXTURE, TextureMapping.getBlockTexture(block)).put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block, "_particle"));
    }

    public static TextureMapping cubeBottomTop(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping cubeBottomTopWithWall(Block block) {
        Material side = TextureMapping.getBlockTexture(block);
        return new TextureMapping().put(TextureSlot.WALL, side).put(TextureSlot.SIDE, side).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping columnWithWall(Block block) {
        Material side = TextureMapping.getBlockTexture(block);
        return new TextureMapping().put(TextureSlot.TEXTURE, side).put(TextureSlot.WALL, side).put(TextureSlot.SIDE, side).put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping door(Material top, Material bottom) {
        return new TextureMapping().put(TextureSlot.TOP, top).put(TextureSlot.BOTTOM, bottom);
    }

    public static TextureMapping door(Block block) {
        return new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping bed(Block block, BedPart part) {
        TextureMapping mapping = new TextureMapping().put(TextureSlot.UP, TextureMapping.getBlockTexture(block, "_" + String.valueOf(part) + "_up")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(block, "_" + String.valueOf(part) + "_east")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(block, "_" + String.valueOf(part) + "_west"));
        if (part == BedPart.FOOT) {
            mapping.put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(block, "_" + String.valueOf(part) + "_south"));
        }
        return mapping;
    }

    public static TextureMapping particle(Block block) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping particle(Material id) {
        return new TextureMapping().put(TextureSlot.PARTICLE, id);
    }

    public static TextureMapping fire0(Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, TextureMapping.getBlockTexture(block, "_0"));
    }

    public static TextureMapping fire1(Block block) {
        return new TextureMapping().put(TextureSlot.FIRE, TextureMapping.getBlockTexture(block, "_1"));
    }

    public static TextureMapping lantern(Block block) {
        return new TextureMapping().put(TextureSlot.LANTERN, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping torch(Block block) {
        return new TextureMapping().put(TextureSlot.TORCH, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping torch(Material id) {
        return new TextureMapping().put(TextureSlot.TORCH, id);
    }

    public static TextureMapping trialSpawner(Block block, String sideSuffix, String topSuffix) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, sideSuffix)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, topSuffix)).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping vault(Block block, String frontSuffix, String sideSuffix, String topSuffix, String bottomSuffix) {
        return new TextureMapping().put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, frontSuffix)).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, sideSuffix)).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, topSuffix)).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, bottomSuffix));
    }

    public static TextureMapping particleFromItem(Item item) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getItemTexture(item));
    }

    public static TextureMapping commandBlock(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front")).put(TextureSlot.BACK, TextureMapping.getBlockTexture(block, "_back"));
    }

    public static TextureMapping orientableCube(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(block, "_bottom"));
    }

    public static TextureMapping orientableCubeOnlyTop(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping orientableCubeSameEnds(Block block) {
        return new TextureMapping().put(TextureSlot.SIDE, TextureMapping.getBlockTexture(block, "_side")).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front")).put(TextureSlot.END, TextureMapping.getBlockTexture(block, "_end"));
    }

    public static TextureMapping top(Block block) {
        return new TextureMapping().put(TextureSlot.TOP, TextureMapping.getBlockTexture(block, "_top"));
    }

    public static TextureMapping craftingTable(Block table, Block bottomWood) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(table, "_front")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(bottomWood)).put(TextureSlot.UP, TextureMapping.getBlockTexture(table, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(table, "_front")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(table, "_side")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(table, "_side")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(table, "_front"));
    }

    public static TextureMapping fletchingTable(Block table, Block bottomWood) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(table, "_front")).put(TextureSlot.DOWN, TextureMapping.getBlockTexture(bottomWood)).put(TextureSlot.UP, TextureMapping.getBlockTexture(table, "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(table, "_front")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(table, "_front")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(table, "_side")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(table, "_side"));
    }

    public static TextureMapping snifferEgg(String suffix) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_north")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_bottom")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_north")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_south")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_east")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.SNIFFER_EGG, suffix + "_west"));
    }

    public static TextureMapping driedGhast(String suffix) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_north")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_bottom")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_top")).put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_north")).put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_south")).put(TextureSlot.EAST, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_east")).put(TextureSlot.WEST, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_west")).put(TextureSlot.TENTACLES, TextureMapping.getBlockTexture(Blocks.DRIED_GHAST, suffix + "_tentacles"));
    }

    public static TextureMapping campfire(Block campfire) {
        return new TextureMapping().put(TextureSlot.LIT_LOG, TextureMapping.getBlockTexture(campfire, "_log_lit")).put(TextureSlot.FIRE, TextureMapping.getBlockTexture(campfire, "_fire"));
    }

    public static TextureMapping candleCake(Block block, boolean lit) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CAKE, "_side")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.CAKE, "_bottom")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.CAKE, "_top")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CAKE, "_side")).put(TextureSlot.CANDLE, TextureMapping.getBlockTexture(block, lit ? "_lit" : ""));
    }

    public static TextureMapping cauldron(Material contentTextureLoc) {
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.CAULDRON, "_side")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.CAULDRON, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.CAULDRON, "_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.CAULDRON, "_bottom")).put(TextureSlot.INSIDE, TextureMapping.getBlockTexture(Blocks.CAULDRON, "_inner")).put(TextureSlot.CONTENT, contentTextureLoc);
    }

    public static TextureMapping sculkShrieker(boolean canSummon) {
        String innerTopString = canSummon ? "_can_summon" : "";
        return new TextureMapping().put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom")).put(TextureSlot.SIDE, TextureMapping.getBlockTexture(Blocks.SCULK_SHRIEKER, "_side")).put(TextureSlot.TOP, TextureMapping.getBlockTexture(Blocks.SCULK_SHRIEKER, "_top")).put(TextureSlot.INNER_TOP, TextureMapping.getBlockTexture(Blocks.SCULK_SHRIEKER, innerTopString + "_inner_top")).put(TextureSlot.BOTTOM, TextureMapping.getBlockTexture(Blocks.SCULK_SHRIEKER, "_bottom"));
    }

    public static TextureMapping bars(Block block) {
        return new TextureMapping().put(TextureSlot.BARS, TextureMapping.getBlockTexture(block)).put(TextureSlot.EDGE, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping layer0(Item item) {
        return new TextureMapping().put(TextureSlot.LAYER0, TextureMapping.getItemTexture(item));
    }

    public static TextureMapping layer0(Block block) {
        return new TextureMapping().put(TextureSlot.LAYER0, TextureMapping.getBlockTexture(block));
    }

    public static TextureMapping layer0(Material id) {
        return new TextureMapping().put(TextureSlot.LAYER0, id);
    }

    public static TextureMapping layered(Material layer0, Material layer1) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1);
    }

    public static TextureMapping layered(Material layer0, Material layer1, Material layer2) {
        return new TextureMapping().put(TextureSlot.LAYER0, layer0).put(TextureSlot.LAYER1, layer1).put(TextureSlot.LAYER2, layer2);
    }

    public static Material getBlockTexture(Block block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return new Material(id.withPrefix("block/"));
    }

    public static Material getBlockTexture(Block block, String suffix) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return TextureMapping.getBlockTexture(id, suffix);
    }

    private static Material getBlockTexture(Identifier id, String suffix) {
        return new Material(id.withPath(path -> "block/" + path + suffix));
    }

    public static Material getItemTexture(Item block) {
        Identifier id = BuiltInRegistries.ITEM.getKey(block);
        return new Material(id.withPrefix("item/"));
    }

    public static Material getItemTexture(Item item, String suffix) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return new Material(id.withPath(path -> "item/" + path + suffix));
    }
}

