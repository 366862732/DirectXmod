/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  io.netty.buffer.ByteBuf
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.block.entity;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FailedTestTracker;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class TestInstanceBlockEntity
extends BlockEntity
implements BoundingBoxRenderable,
BeaconBeamOwner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component INVALID_TEST_NAME = Component.translatable("test_instance_block.invalid_test");
    private static final List<BeaconBeamOwner.Section> BEAM_CLEARED = List.of();
    private static final List<BeaconBeamOwner.Section> BEAM_RUNNING = List.of(new BeaconBeamOwner.Section(ARGB.color(128, 128, 128)));
    private static final List<BeaconBeamOwner.Section> BEAM_SUCCESS = List.of(new BeaconBeamOwner.Section(ARGB.color(0, 255, 0)));
    private static final List<BeaconBeamOwner.Section> BEAM_REQUIRED_FAILED = List.of(new BeaconBeamOwner.Section(ARGB.color(255, 0, 0)));
    private static final List<BeaconBeamOwner.Section> BEAM_OPTIONAL_FAILED = List.of(new BeaconBeamOwner.Section(ARGB.color(255, 128, 0)));
    private static final Vec3i STRUCTURE_OFFSET = new Vec3i(0, 1, 1);
    private Data data;
    private final List<ErrorMarker> errorMarkers = new ArrayList<ErrorMarker>();

    public TestInstanceBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.TEST_INSTANCE_BLOCK, worldPosition, blockState);
        this.data = new Data(Optional.empty(), Vec3i.ZERO, Rotation.NONE, false, Status.CLEARED, Optional.empty());
    }

    public void set(Data data) {
        this.data = data;
        this.setChanged();
    }

    public static Optional<Vec3i> getStructureSize(ServerLevel level, ResourceKey<GameTestInstance> testKey) {
        return TestInstanceBlockEntity.getStructureTemplate(level, testKey).map(StructureTemplate::getSize);
    }

    public BoundingBox getStructureBoundingBox() {
        BlockPos corner1 = this.getStructurePos();
        BlockPos corner2 = corner1.offset(this.getTransformedSize()).offset(-1, -1, -1);
        return BoundingBox.fromCorners(corner1, corner2);
    }

    public BoundingBox getTestBoundingBox() {
        return this.getStructureBoundingBox().inflatedBy(this.getPadding());
    }

    public AABB getStructureBounds() {
        return AABB.of(this.getStructureBoundingBox());
    }

    public AABB getTestBounds() {
        return this.getStructureBounds().inflate(this.getPadding());
    }

    private static Optional<StructureTemplate> getStructureTemplate(ServerLevel level, ResourceKey<GameTestInstance> testKey) {
        return level.registryAccess().get(testKey).map(test -> ((GameTestInstance)test.value()).structure()).flatMap(template -> level.getStructureManager().get((Identifier)template));
    }

    public Optional<ResourceKey<GameTestInstance>> test() {
        return this.data.test();
    }

    public Component getTestName() {
        return this.test().map(key -> Component.literal(key.identifier().toString())).orElse(INVALID_TEST_NAME);
    }

    private Optional<Holder.Reference<GameTestInstance>> getTestHolder() {
        return this.test().flatMap(this.level.registryAccess()::get);
    }

    public boolean ignoreEntities() {
        return this.data.ignoreEntities();
    }

    public Vec3i getSize() {
        return this.data.size();
    }

    public Rotation getRotation() {
        return this.getTestHolder().map(Holder::value).map(GameTestInstance::rotation).orElse(Rotation.NONE).getRotated(this.data.rotation());
    }

    public Optional<Component> errorMessage() {
        return this.data.errorMessage();
    }

    public void setErrorMessage(Component errorMessage) {
        this.set(this.data.withError(errorMessage));
    }

    public void setSuccess() {
        this.set(this.data.withStatus(Status.FINISHED));
    }

    public void setRunning() {
        this.set(this.data.withStatus(Status.RUNNING));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level instanceof ServerLevel) {
            this.level.sendBlockUpdated(this.getBlockPos(), Blocks.AIR.defaultBlockState(), this.getBlockState(), 3);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read("data", Data.CODEC).ifPresent(this::set);
        this.errorMarkers.clear();
        this.errorMarkers.addAll(input.read("errors", ErrorMarker.LIST_CODEC).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store("data", Data.CODEC, this.data);
        if (!this.errorMarkers.isEmpty()) {
            output.store("errors", ErrorMarker.LIST_CODEC, this.errorMarkers);
        }
    }

    @Override
    public BoundingBoxRenderable.Mode renderMode() {
        return BoundingBoxRenderable.Mode.BOX;
    }

    public BlockPos getStructurePos() {
        int padding = this.getPadding();
        return TestInstanceBlockEntity.getStructurePos(this.getBlockPos().offset(padding, padding, padding));
    }

    public static BlockPos getStructurePos(BlockPos blockPos) {
        return blockPos.offset(STRUCTURE_OFFSET);
    }

    @Override
    public BoundingBoxRenderable.RenderableBox getRenderableBox() {
        int padding = this.getPadding();
        return new BoundingBoxRenderable.RenderableBox(new BlockPos(STRUCTURE_OFFSET).offset(padding, padding, padding), this.getTransformedSize());
    }

    @Override
    public List<BeaconBeamOwner.Section> getBeamSections() {
        return switch (this.data.status().ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> BEAM_CLEARED;
            case 1 -> BEAM_RUNNING;
            case 2 -> this.errorMessage().isEmpty() ? BEAM_SUCCESS : (this.getTestHolder().map(Holder::value).map(GameTestInstance::required).orElse(true) != false ? BEAM_REQUIRED_FAILED : BEAM_OPTIONAL_FAILED);
        };
    }

    private Vec3i getTransformedSize() {
        Vec3i size = this.getSize();
        Rotation rotation = this.getRotation();
        boolean axesSwitched = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        int xSize = axesSwitched ? size.getZ() : size.getX();
        int zSize = axesSwitched ? size.getX() : size.getZ();
        return new Vec3i(xSize, size.getY(), zSize);
    }

    public void resetTest(Consumer<Component> feedbackOutput) {
        this.removeBarriers();
        this.clearErrorMarkers();
        boolean placed = this.placeStructure();
        if (placed) {
            feedbackOutput.accept(Component.translatable("test_instance_block.reset_success", this.getTestName()).withStyle(ChatFormatting.GREEN));
        }
        this.set(this.data.withStatus(Status.CLEARED));
    }

    public Optional<Identifier> saveTest(Consumer<Component> feedbackOutput) {
        Optional<Holder.Reference<GameTestInstance>> test = this.getTestHolder();
        Optional<Identifier> identifier = test.isPresent() ? Optional.of(test.get().value().structure()) : this.test().map(ResourceKey::identifier);
        if (identifier.isEmpty()) {
            BlockPos pos = this.getBlockPos();
            feedbackOutput.accept(Component.translatable("test_instance_block.error.unable_to_save", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.RED));
            return identifier;
        }
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            StructureBlockEntity.saveStructure(serverLevel, identifier.get(), this.getStructurePos(), this.getSize(), this.ignoreEntities(), "", true, List.of(Blocks.AIR));
        }
        return identifier;
    }

    public boolean exportTest(Consumer<Component> feedbackOutput) {
        Level level;
        Optional<Identifier> saved = this.saveTest(feedbackOutput);
        if (saved.isEmpty() || !((level = this.level) instanceof ServerLevel)) {
            return false;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        return TestInstanceBlockEntity.export(serverLevel, saved.get(), feedbackOutput);
    }

    public static boolean export(ServerLevel level, Identifier structureId, Consumer<Component> feedbackOutput) {
        StructureTemplateManager structureManager = level.getStructureManager();
        TemplatePathFactory testTemplatePathFactory = structureManager.testTemplates();
        if (testTemplatePathFactory == null) {
            feedbackOutput.accept(Component.literal("Test structure exporting is disabled").withStyle(ChatFormatting.RED));
            return true;
        }
        Optional<StructureTemplate> structureTemplate = structureManager.get(structureId);
        if (structureTemplate.isEmpty()) {
            feedbackOutput.accept(Component.literal("Could not find structure " + String.valueOf(structureId)).withStyle(ChatFormatting.RED));
            return true;
        }
        Path outputFile = testTemplatePathFactory.createAndValidatePathToStructure(structureId, StructureTemplateManager.RESOURCE_TEXT_STRUCTURE_LISTER);
        try {
            StructureTemplateManager.save(outputFile, structureTemplate.get(), true);
        }
        catch (Exception e) {
            LOGGER.error("Failed to save structure file {} to {}", new Object[]{structureId, outputFile, e});
            feedbackOutput.accept(Component.literal("Failed to save structure file " + String.valueOf(structureId) + " to " + String.valueOf(outputFile)).withStyle(ChatFormatting.RED));
            return true;
        }
        feedbackOutput.accept(Component.literal("Exported " + String.valueOf(structureId) + " to " + String.valueOf(outputFile.toAbsolutePath())));
        return false;
    }

    public void runTest(Consumer<Component> feedbackOutput) {
        Level level = this.level;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Optional<Holder.Reference<GameTestInstance>> test = this.getTestHolder();
        BlockPos pos = this.getBlockPos();
        if (test.isEmpty()) {
            feedbackOutput.accept(Component.translatable("test_instance_block.error.no_test", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.RED));
            return;
        }
        if (!this.placeStructure()) {
            feedbackOutput.accept(Component.translatable("test_instance_block.error.no_test_structure", pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.RED));
            return;
        }
        this.clearErrorMarkers();
        GameTestTicker.SINGLETON.clear();
        FailedTestTracker.forgetFailedTests();
        feedbackOutput.accept(Component.translatable("test_instance_block.starting", test.get().getRegisteredName()));
        GameTestInfo gameTestInfo = new GameTestInfo(test.get(), this.data.rotation(), serverLevel, RetryOptions.noRetries());
        gameTestInfo.setTestBlockPos(pos);
        GameTestRunner runner = GameTestRunner.Builder.fromInfo(List.of(gameTestInfo), serverLevel).build();
        TestCommand.trackAndStartRunner(serverLevel.getServer().createCommandSourceStack(), runner);
    }

    public boolean placeStructure() {
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            Optional template = this.data.test().flatMap(test -> TestInstanceBlockEntity.getStructureTemplate(serverLevel, test));
            if (template.isPresent()) {
                this.placeStructure(serverLevel, (StructureTemplate)template.get());
                return true;
            }
        }
        return false;
    }

    private void placeStructure(ServerLevel level, StructureTemplate template) {
        StructurePlaceSettings placeSettings = new StructurePlaceSettings().setRotation(this.getRotation()).setIgnoreEntities(this.data.ignoreEntities()).setKnownShape(true);
        BlockPos pos = this.getStartCorner();
        this.forceLoadChunks();
        int padding = this.getPadding();
        StructureUtils.clearSpaceForStructure(this.getTestBoundingBox(), level);
        this.removeEntities();
        template.placeInWorld(level, pos, pos, placeSettings, level.getRandom(), 818);
    }

    private int getPadding() {
        return this.getTestHolder().map(r -> ((GameTestInstance)r.value()).padding()).orElse(0);
    }

    private void removeEntities() {
        this.level.getEntities(null, this.getTestBounds()).stream().filter(entity -> !(entity instanceof Player)).forEach(Entity::discard);
    }

    private void forceLoadChunks() {
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            this.getStructureBoundingBox().intersectingChunks().forEach(pos -> serverLevel.setChunkForced(pos.x(), pos.z(), true));
        }
    }

    public BlockPos getStartCorner() {
        Vec3i structureSize = this.getSize();
        Rotation rotation = this.getRotation();
        BlockPos northWestCorner = this.getStructurePos();
        return switch (rotation) {
            default -> throw new MatchException(null, null);
            case Rotation.NONE -> northWestCorner;
            case Rotation.CLOCKWISE_90 -> northWestCorner.offset(structureSize.getZ() - 1, 0, 0);
            case Rotation.CLOCKWISE_180 -> northWestCorner.offset(structureSize.getX() - 1, 0, structureSize.getZ() - 1);
            case Rotation.COUNTERCLOCKWISE_90 -> northWestCorner.offset(0, 0, structureSize.getX() - 1);
        };
    }

    public void encaseStructure() {
        this.processStructureBoundary(blockPos -> {
            if (!this.level.getBlockState((BlockPos)blockPos).is(Blocks.TEST_INSTANCE_BLOCK)) {
                this.level.setBlockAndUpdate((BlockPos)blockPos, Blocks.BARRIER.defaultBlockState());
            }
        });
    }

    public void removeBarriers() {
        this.processStructureBoundary(blockPos -> {
            if (this.level.getBlockState((BlockPos)blockPos).is(Blocks.BARRIER)) {
                this.level.setBlockAndUpdate((BlockPos)blockPos, Blocks.AIR.defaultBlockState());
            }
        });
    }

    public void processStructureBoundary(Consumer<BlockPos> action) {
        AABB bounds = this.getStructureBounds();
        boolean hasCeiling = this.getTestHolder().map(h -> ((GameTestInstance)h.value()).skyAccess()).orElse(false) == false;
        BlockPos low = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).offset(-1, -1, -1);
        BlockPos high = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ);
        BlockPos.betweenClosedStream(low, high).forEach(blockPos -> {
            boolean isCeiling;
            boolean isNonCeilingEdge = blockPos.getX() == low.getX() || blockPos.getX() == high.getX() || blockPos.getZ() == low.getZ() || blockPos.getZ() == high.getZ() || blockPos.getY() == low.getY();
            boolean bl = isCeiling = blockPos.getY() == high.getY();
            if (isNonCeilingEdge || isCeiling && hasCeiling) {
                action.accept((BlockPos)blockPos);
            }
        });
    }

    public void markError(BlockPos pos, Component text) {
        this.errorMarkers.add(new ErrorMarker(pos, text));
        this.setChanged();
    }

    public void clearErrorMarkers() {
        if (!this.errorMarkers.isEmpty()) {
            this.errorMarkers.clear();
            this.setChanged();
        }
    }

    public List<ErrorMarker> getErrorMarkers() {
        return this.errorMarkers;
    }

    public record Data(Optional<ResourceKey<GameTestInstance>> test, Vec3i size, Rotation rotation, boolean ignoreEntities, Status status, Optional<Component> errorMessage) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group((App)ResourceKey.codec(Registries.TEST_INSTANCE).optionalFieldOf("test").forGetter(Data::test), (App)Vec3i.CODEC.fieldOf("size").forGetter(Data::size), (App)Rotation.CODEC.fieldOf("rotation").forGetter(Data::rotation), (App)Codec.BOOL.fieldOf("ignore_entities").forGetter(Data::ignoreEntities), (App)Status.CODEC.fieldOf("status").forGetter(Data::status), (App)ComponentSerialization.CODEC.optionalFieldOf("error_message").forGetter(Data::errorMessage)).apply((Applicative)i, Data::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.TEST_INSTANCE)), Data::test, Vec3i.STREAM_CODEC, Data::size, Rotation.STREAM_CODEC, Data::rotation, ByteBufCodecs.BOOL, Data::ignoreEntities, Status.STREAM_CODEC, Data::status, ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC), Data::errorMessage, Data::new);

        public Data withSize(Vec3i size) {
            return new Data(this.test, size, this.rotation, this.ignoreEntities, this.status, this.errorMessage);
        }

        public Data withStatus(Status status) {
            return new Data(this.test, this.size, this.rotation, this.ignoreEntities, status, Optional.empty());
        }

        public Data withError(Component error) {
            return new Data(this.test, this.size, this.rotation, this.ignoreEntities, Status.FINISHED, Optional.of(error));
        }
    }

    public static enum Status implements StringRepresentable
    {
        CLEARED("cleared", 0),
        RUNNING("running", 1),
        FINISHED("finished", 2);

        private static final IntFunction<Status> ID_MAP;
        public static final Codec<Status> CODEC;
        public static final StreamCodec<ByteBuf, Status> STREAM_CODEC;
        private final String id;
        private final int index;

        private Status(String id, int index) {
            this.id = id;
            this.index = index;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public static Status byIndex(int index) {
            return ID_MAP.apply(index);
        }

        static {
            ID_MAP = ByIdMap.continuous(s -> s.index, Status.values(), ByIdMap.OutOfBoundsStrategy.ZERO);
            CODEC = StringRepresentable.fromEnum(Status::values);
            STREAM_CODEC = ByteBufCodecs.idMapper(Status::byIndex, s -> s.index);
        }
    }

    public record ErrorMarker(BlockPos pos, Component text) {
        public static final Codec<ErrorMarker> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockPos.CODEC.fieldOf("pos").forGetter(ErrorMarker::pos), (App)ComponentSerialization.CODEC.fieldOf("text").forGetter(ErrorMarker::text)).apply((Applicative)i, ErrorMarker::new));
        public static final Codec<List<ErrorMarker>> LIST_CODEC = CODEC.listOf();
    }
}

