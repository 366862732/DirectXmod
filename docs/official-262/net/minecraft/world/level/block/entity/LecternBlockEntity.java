/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import java.util.Objects;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LecternBlockEntity
extends BlockEntity
implements Clearable,
MenuProvider {
    public static final int DATA_PAGE = 0;
    public static final int NUM_DATA = 1;
    public static final int SLOT_BOOK = 0;
    public static final int NUM_SLOTS = 1;
    private final Container bookAccess = new Container(this){
        final /* synthetic */ LecternBlockEntity this$0;
        {
            LecternBlockEntity lecternBlockEntity = this$0;
            Objects.requireNonNull(lecternBlockEntity);
            this.this$0 = lecternBlockEntity;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.this$0.book.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? this.this$0.book : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            if (slot == 0) {
                ItemStack result = this.this$0.book.split(count);
                if (this.this$0.book.isEmpty()) {
                    this.this$0.onBookItemRemove();
                }
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot == 0) {
                ItemStack prev = this.this$0.book;
                this.this$0.book = ItemStack.EMPTY;
                this.this$0.onBookItemRemove();
                return prev;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
            this.this$0.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this.this$0, player) && this.this$0.hasBook();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack itemStack) {
            return false;
        }

        @Override
        public void clearContent() {
        }
    };
    private final ContainerData dataAccess = new ContainerData(this){
        final /* synthetic */ LecternBlockEntity this$0;
        {
            LecternBlockEntity lecternBlockEntity = this$0;
            Objects.requireNonNull(lecternBlockEntity);
            this.this$0 = lecternBlockEntity;
        }

        @Override
        public int get(int dataId) {
            return dataId == 0 ? this.this$0.page : 0;
        }

        @Override
        public void set(int dataId, int value) {
            if (dataId == 0) {
                this.this$0.setPage(value);
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };
    private ItemStack book = ItemStack.EMPTY;
    private int page;
    private int pageCount;

    public LecternBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.LECTERN, worldPosition, blockState);
    }

    public ItemStack getBook() {
        return this.book;
    }

    public boolean hasBook() {
        return this.book.has(DataComponents.WRITABLE_BOOK_CONTENT) || this.book.has(DataComponents.WRITTEN_BOOK_CONTENT);
    }

    public void setBook(ItemStack book) {
        this.setBook(book, null);
    }

    private void onBookItemRemove() {
        this.page = 0;
        this.pageCount = 0;
        LecternBlock.resetBookState(null, this.getLevel(), this.getBlockPos(), this.getBlockState(), false);
    }

    public void setBook(ItemStack book, @Nullable Player resolutionContext) {
        this.book = this.resolveBook(book, resolutionContext);
        this.page = 0;
        this.pageCount = LecternBlockEntity.getPageCount(this.book);
        this.setChanged();
    }

    private void setPage(int page) {
        int newPage = Mth.clamp(page, 0, this.pageCount - 1);
        if (newPage != this.page) {
            this.page = newPage;
            this.setChanged();
            LecternBlock.signalPageChange(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public int getPage() {
        return this.page;
    }

    public int getRedstoneSignal() {
        float pageProgress = this.pageCount > 1 ? (float)this.getPage() / ((float)this.pageCount - 1.0f) : 1.0f;
        return Mth.floor(pageProgress * 14.0f) + (this.hasBook() ? 1 : 0);
    }

    private ItemStack resolveBook(ItemStack book, @Nullable Player player) {
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            ResolutionContext context = ResolutionContext.create(this.createCommandSourceStack(player, serverLevel));
            WrittenBookContent.resolveForItem(book, context, this.level.registryAccess());
        }
        return book;
    }

    private CommandSourceStack createCommandSourceStack(@Nullable Player player, ServerLevel level) {
        Component displayName;
        String textName;
        if (player == null) {
            textName = "Lectern";
            displayName = Component.literal("Lectern");
        } else {
            textName = player.getPlainTextName();
            displayName = player.getDisplayName();
        }
        Vec3 pos = Vec3.atCenterOf(this.worldPosition);
        return new CommandSourceStack(CommandSource.NULL, pos, Vec2.ZERO, level, LevelBasedPermissionSet.GAMEMASTER, textName, displayName, level.getServer(), player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.book = input.read("Book", ItemStack.CODEC).map(book -> this.resolveBook((ItemStack)book, null)).orElse(ItemStack.EMPTY);
        this.pageCount = LecternBlockEntity.getPageCount(this.book);
        this.page = Mth.clamp(input.getIntOr("Page", 0), 0, this.pageCount - 1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.getBook().isEmpty()) {
            output.store("Book", ItemStack.CODEC, this.getBook());
            output.putInt("Page", this.page);
        }
    }

    @Override
    public void clearContent() {
        this.setBook(ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (state.getValue(LecternBlock.HAS_BOOK).booleanValue() && this.level != null) {
            Direction direction = state.getValue(LecternBlock.FACING);
            ItemStack book = this.getBook().copy();
            float xo = 0.25f * (float)direction.getStepX();
            float zo = 0.25f * (float)direction.getStepZ();
            ItemEntity entity = new ItemEntity(this.level, (double)pos.getX() + 0.5 + (double)xo, pos.getY() + 1, (double)pos.getZ() + 0.5 + (double)zo, book);
            entity.setDefaultPickUpDelay();
            this.level.addFreshEntity(entity);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LecternMenu(containerId, this.bookAccess, this.dataAccess);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.lectern");
    }

    private static int getPageCount(ItemStack book) {
        WrittenBookContent writtenContent = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenContent != null) {
            return writtenContent.pages().size();
        }
        WritableBookContent writableContent = book.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writableContent != null) {
            return writableContent.pages().size();
        }
        return 0;
    }
}

