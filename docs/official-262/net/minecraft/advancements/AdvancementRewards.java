/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.advancements;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record AdvancementRewards(int experience, List<ResourceKey<LootTable>> loot, List<ResourceKey<Recipe<?>>> recipes, Optional<CacheableFunction> function) {
    public static final Codec<AdvancementRewards> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.INT.optionalFieldOf("experience", (Object)0).forGetter(AdvancementRewards::experience), (App)LootTable.KEY_CODEC.listOf().optionalFieldOf("loot", List.of()).forGetter(AdvancementRewards::loot), (App)Recipe.KEY_CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(AdvancementRewards::recipes), (App)CacheableFunction.CODEC.optionalFieldOf("function").forGetter(AdvancementRewards::function)).apply((Applicative)i, AdvancementRewards::new));
    public static final AdvancementRewards EMPTY = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

    public void grant(ServerPlayer player) {
        player.giveExperiencePoints(this.experience);
        ServerLevel level = player.level();
        MinecraftServer server = level.getServer();
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.ORIGIN, player.position()).create(LootContextParamSets.ADVANCEMENT_REWARD);
        boolean changes = false;
        for (ResourceKey<LootTable> lootTable : this.loot) {
            for (ItemStack itemStack : server.reloadableRegistries().getLootTable(lootTable).getRandomItems(params)) {
                if (player.addItem(itemStack)) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                    changes = true;
                    continue;
                }
                ItemEntity drop = player.drop(itemStack, false);
                if (drop == null) continue;
                drop.setNoPickUpDelay();
                drop.setTarget(player.getUUID());
            }
        }
        if (changes) {
            player.containerMenu.broadcastChanges();
        }
        if (!this.recipes.isEmpty()) {
            player.awardRecipesByKey(this.recipes);
        }
        this.function.flatMap(function -> function.get(server.getFunctions())).ifPresent(function -> server.getFunctions().execute((CommandFunction<CommandSourceStack>)function, player.createCommandSourceStack().withSuppressedOutput().withPermission(LevelBasedPermissionSet.GAMEMASTER)));
    }

    public static class Builder {
        private int experience;
        private final ImmutableList.Builder<ResourceKey<LootTable>> loot = ImmutableList.builder();
        private final ImmutableList.Builder<ResourceKey<Recipe<?>>> recipes = ImmutableList.builder();
        private Optional<Identifier> function = Optional.empty();

        public static Builder experience(int amount) {
            return new Builder().addExperience(amount);
        }

        public Builder addExperience(int amount) {
            this.experience += amount;
            return this;
        }

        public static Builder loot(ResourceKey<LootTable> id) {
            return new Builder().addLootTable(id);
        }

        public Builder addLootTable(ResourceKey<LootTable> id) {
            this.loot.add(id);
            return this;
        }

        public static Builder recipe(ResourceKey<Recipe<?>> id) {
            return new Builder().addRecipe(id);
        }

        public Builder addRecipe(ResourceKey<Recipe<?>> id) {
            this.recipes.add(id);
            return this;
        }

        public static Builder function(Identifier id) {
            return new Builder().runs(id);
        }

        public Builder runs(Identifier function) {
            this.function = Optional.of(function);
            return this;
        }

        public AdvancementRewards build() {
            return new AdvancementRewards(this.experience, (List<ResourceKey<LootTable>>)this.loot.build(), (List<ResourceKey<Recipe<?>>>)this.recipes.build(), this.function.map(CacheableFunction::new));
        }
    }
}

