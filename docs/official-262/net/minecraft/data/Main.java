/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  joptsimple.AbstractOptionSpec
 *  joptsimple.ArgumentAcceptingOptionSpec
 *  joptsimple.OptionParser
 *  joptsimple.OptionSet
 *  joptsimple.OptionSpec
 *  joptsimple.OptionSpecBuilder
 */
package net.minecraft.data;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import joptsimple.AbstractOptionSpec;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.info.BiomeParametersDumpReport;
import net.minecraft.data.info.BlockListReport;
import net.minecraft.data.info.CommandsReport;
import net.minecraft.data.info.DatapackStructureReport;
import net.minecraft.data.info.PacketReport;
import net.minecraft.data.info.RegistryComponentsReport;
import net.minecraft.data.info.RegistryDumpReport;
import net.minecraft.data.loot.packs.TradeRebalanceLootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.TradeRebalanceRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.DialogTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FeatureTagsProvider;
import net.minecraft.data.tags.FlatLevelGeneratorPresetTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.GameEventTagsProvider;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.data.tags.PotionTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.TimelineTagsProvider;
import net.minecraft.data.tags.TradeRebalanceEnchantmentTagsProvider;
import net.minecraft.data.tags.TradeRebalanceTradeTagsProvider;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.data.tags.VanillaEnchantmentTagsProvider;
import net.minecraft.data.tags.VanillaItemTagsProvider;
import net.minecraft.data.tags.VillagerTradesTagsProvider;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.dataprovider.JsonRpcApiSchema;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;

public class Main {
    @SuppressForbidden(reason="System.out needed before bootstrap")
    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser parser = new OptionParser();
        AbstractOptionSpec helpOption = parser.accepts("help", "Show the help menu").forHelp();
        OptionSpecBuilder serverOption = parser.accepts("server", "Include server generators");
        OptionSpecBuilder devOption = parser.accepts("dev", "Include development tools");
        OptionSpecBuilder reportsOption = parser.accepts("reports", "Include data reports");
        parser.accepts("validate", "Validate inputs");
        OptionSpecBuilder allOption = parser.accepts("all", "Include all generators");
        ArgumentAcceptingOptionSpec outputOption = parser.accepts("output", "Output folder").withRequiredArg().defaultsTo((Object)"generated", (Object[])new String[0]);
        ArgumentAcceptingOptionSpec inputOption = parser.accepts("input", "Input folder").withRequiredArg();
        OptionSet optionSet = parser.parse(args);
        if (optionSet.has((OptionSpec)helpOption) || !optionSet.hasOptions()) {
            parser.printHelpOn((OutputStream)System.out);
            return;
        }
        Path output = Paths.get((String)outputOption.value(optionSet), new String[0]);
        boolean allOptions = optionSet.has((OptionSpec)allOption);
        boolean server = allOptions || optionSet.has((OptionSpec)serverOption);
        boolean dev = allOptions || optionSet.has((OptionSpec)devOption);
        boolean reports = allOptions || optionSet.has((OptionSpec)reportsOption);
        List<Path> input = optionSet.valuesOf((OptionSpec)inputOption).stream().map(x$0 -> Paths.get(x$0, new String[0])).toList();
        DataGenerator.Cached generator = new DataGenerator.Cached(output, SharedConstants.getCurrentVersion(), true);
        Main.addServerDefinitionProviders(generator, server, reports);
        Main.addServerConverters(generator, input, server, dev);
        ((DataGenerator)generator).run();
        Util.shutdownExecutors();
    }

    private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> target, CompletableFuture<HolderLookup.Provider> registries) {
        return output -> (DataProvider)target.apply(output, registries);
    }

    public static void addServerConverters(DataGenerator generator, Collection<Path> input, boolean server, boolean dev) {
        DataGenerator.PackGenerator commonVanillaPack = generator.getVanillaPack(server);
        commonVanillaPack.addProvider(o -> new SnbtToNbt(o, input).addFilter(new StructureUpdater()));
        DataGenerator.PackGenerator devVanillaPack = generator.getVanillaPack(dev);
        devVanillaPack.addProvider(o -> new NbtToSnbt(o, input));
    }

    public static void addServerDefinitionProviders(DataGenerator generator, boolean server, boolean reports) {
        CompletableFuture<HolderLookup.Provider> vanillaRegistries = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        DataGenerator.PackGenerator serverVanillaPack = generator.getVanillaPack(server);
        serverVanillaPack.addProvider(Main.bindRegistries(RegistriesDatapackGenerator::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(VanillaAdvancementProvider::create, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(VanillaLootTableProvider::create, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(VanillaRecipeProvider.Runner::new, vanillaRegistries));
        TagsProvider vanillaBlockTagsProvider = serverVanillaPack.addProvider(Main.bindRegistries(VanillaBlockTagsProvider::new, vanillaRegistries));
        TagsProvider vanillaItemTagsProvider = serverVanillaPack.addProvider(Main.bindRegistries(VanillaItemTagsProvider::new, vanillaRegistries));
        TagsProvider vanillaBiomeTagsProvider = serverVanillaPack.addProvider(Main.bindRegistries(BiomeTagsProvider::new, vanillaRegistries));
        TagsProvider vanillaBannerPatternTagsProvider = serverVanillaPack.addProvider(Main.bindRegistries(BannerPatternTagsProvider::new, vanillaRegistries));
        TagsProvider vanillaStructureTagsProvider = serverVanillaPack.addProvider(Main.bindRegistries(StructureTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(DamageTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(DialogTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(EntityTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(FlatLevelGeneratorPresetTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(FluidTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(GameEventTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(InstrumentTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(PaintingVariantTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(PoiTypeTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(WorldPresetTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(VanillaEnchantmentTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(TimelineTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(PotionTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(VillagerTradesTagsProvider::new, vanillaRegistries));
        serverVanillaPack.addProvider(Main.bindRegistries(FeatureTagsProvider::new, vanillaRegistries));
        DataGenerator.PackGenerator reportsVanillaPack = generator.getVanillaPack(reports);
        reportsVanillaPack.addProvider(Main.bindRegistries(BiomeParametersDumpReport::new, vanillaRegistries));
        reportsVanillaPack.addProvider(Main.bindRegistries(RegistryComponentsReport::new, vanillaRegistries));
        reportsVanillaPack.addProvider(Main.bindRegistries(BlockListReport::new, vanillaRegistries));
        reportsVanillaPack.addProvider(Main.bindRegistries(CommandsReport::new, vanillaRegistries));
        reportsVanillaPack.addProvider(RegistryDumpReport::new);
        reportsVanillaPack.addProvider(PacketReport::new);
        reportsVanillaPack.addProvider(DatapackStructureReport::new);
        reportsVanillaPack.addProvider(JsonRpcApiSchema::new);
        CompletableFuture<RegistrySetBuilder.PatchedRegistries> tradeRebalanceRegistries = TradeRebalanceRegistries.createLookup(vanillaRegistries);
        CompletionStage patchedRegistrySet = tradeRebalanceRegistries.thenApply(RegistrySetBuilder.PatchedRegistries::patches);
        DataGenerator.PackGenerator tradeRebalancePack = generator.getBuiltinDatapack(server, "trade_rebalance");
        tradeRebalancePack.addProvider(Main.bindRegistries(RegistriesDatapackGenerator::new, (CompletableFuture<HolderLookup.Provider>)patchedRegistrySet));
        tradeRebalancePack.addProvider(o -> PackMetadataGenerator.forFeaturePack(o, Component.translatable("dataPack.trade_rebalance.description"), FeatureFlagSet.of(FeatureFlags.TRADE_REBALANCE)));
        CompletionStage patchedRegistries = tradeRebalanceRegistries.thenApply(RegistrySetBuilder.PatchedRegistries::full);
        tradeRebalancePack.addProvider(Main.bindRegistries(TradeRebalanceLootTableProvider::create, (CompletableFuture<HolderLookup.Provider>)patchedRegistries));
        tradeRebalancePack.addProvider(Main.bindRegistries(TradeRebalanceEnchantmentTagsProvider::new, (CompletableFuture<HolderLookup.Provider>)patchedRegistries));
        tradeRebalancePack.addProvider(Main.bindRegistries(TradeRebalanceTradeTagsProvider::new, (CompletableFuture<HolderLookup.Provider>)patchedRegistries));
        DataGenerator.PackGenerator redstoneChangesPack = generator.getBuiltinDatapack(server, "redstone_experiments");
        redstoneChangesPack.addProvider(o -> PackMetadataGenerator.forFeaturePack(o, Component.translatable("dataPack.redstone_experiments.description"), FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS)));
        DataGenerator.PackGenerator minecartImprovementsPack = generator.getBuiltinDatapack(server, "minecart_improvements");
        minecartImprovementsPack.addProvider(o -> PackMetadataGenerator.forFeaturePack(o, Component.translatable("dataPack.minecart_improvements.description"), FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS)));
    }
}

