package tr.gokhan.easystorage.block.entity;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tr.gokhan.easystorage.block.FilterHopperBlock;
import tr.gokhan.easystorage.menu.FilterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * Block entity for the Filter Hopper. The {@link Container} it exposes is the
 * 5-slot buffer only; the filter lives in a separate 1-slot container that
 * automation never sees. Transfer timing follows the vanilla hopper.
 */
public class FilterHopperBlockEntity extends BaseContainerBlockEntity implements Hopper {
	public static final int BUFFER_SIZE = 5;
	private static final int NO_COOLDOWN = -1;
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.filter_hopper");

	private NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
	private final SimpleContainer filter = new SimpleContainer(1) {
		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			FilterHopperBlockEntity.this.setChanged();
		}
	};
	private int cooldownTime = NO_COOLDOWN;

	public FilterHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FILTER_HOPPER, pos, state);
	}

	public Container getFilter() {
		return this.filter;
	}

	public ItemStack getFilterItem() {
		return this.filter.getItem(0);
	}

	public void setFilterItem(ItemStack stack) {
		this.filter.setItem(0, stack.copyWithCount(1));
	}

	/** True when {@code stack} has the same item type as the filter. Components are ignored. */
	public static boolean matchesFilter(ItemStack filterItem, ItemStack stack) {
		return !filterItem.isEmpty() && stack.is(filterItem.getItem());
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return matchesFilter(this.getFilterItem(), stack);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.filter.setItem(0, input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		this.cooldownTime = input.getIntOr("TransferCooldown", NO_COOLDOWN);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		ItemStack filterItem = this.getFilterItem();
		if (!filterItem.isEmpty()) {
			output.store("filter", ItemStack.CODEC, filterItem);
		}
		output.putInt("TransferCooldown", this.cooldownTime);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level != null) {
			Containers.dropContents(this.level, pos, this.filter);
		}
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new FilterHopperMenu(containerId, inventory, this, this.filter);
	}

	// --- Hopper ---

	@Override
	public double getLevelX() {
		return this.worldPosition.getX() + 0.5;
	}

	@Override
	public double getLevelY() {
		return this.worldPosition.getY() + 0.5;
	}

	@Override
	public double getLevelZ() {
		return this.worldPosition.getZ() + 0.5;
	}

	@Override
	public boolean isGridAligned() {
		return true;
	}

	// --- Ticking (mirrors HopperBlockEntity) ---

	public static void pushItemsTick(Level level, BlockPos pos, BlockState state, FilterHopperBlockEntity filterHopper) {
		filterHopper.cooldownTime--;
		if (!filterHopper.isOnCooldown()) {
			filterHopper.cooldownTime = 0;
			filterHopper.tryMoveItems(level, pos, state, () -> HopperBlockEntity.suckInItems(level, filterHopper));
		}
	}

	public static void entityInside(Level level, BlockPos pos, BlockState state, Entity entity, FilterHopperBlockEntity filterHopper) {
		if (entity instanceof ItemEntity itemEntity
			&& !itemEntity.getItem().isEmpty()
			&& entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(filterHopper.getSuckAabb())) {
			filterHopper.tryMoveItems(level, pos, state, () -> HopperBlockEntity.addItem(filterHopper, itemEntity));
		}
	}

	private boolean tryMoveItems(Level level, BlockPos pos, BlockState state, BooleanSupplier pull) {
		if (level.isClientSide() || this.isOnCooldown() || !state.getValue(FilterHopperBlock.ENABLED)) {
			return false;
		}

		boolean changed = false;
		if (!this.isEmpty()) {
			changed = this.ejectItems(level, pos, state);
		}
		if (!this.isFull()) {
			changed |= pull.getAsBoolean();
		}

		if (changed) {
			this.cooldownTime = HopperBlockEntity.MOVE_ITEM_SPEED;
			setChanged(level, pos, state);
			return true;
		}
		return false;
	}

	private boolean ejectItems(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FilterHopperBlock.FACING);
		Container target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
		if (target == null) {
			return false;
		}

		Direction face = facing.getOpposite();
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			ItemStack stack = this.getItem(slot);
			if (!stack.isEmpty()) {
				int originalCount = stack.getCount();
				ItemStack rest = HopperBlockEntity.addItem(this, target, this.removeItem(slot, 1), face);
				if (rest.isEmpty()) {
					target.setChanged();
					return true;
				}
				stack.setCount(originalCount);
				if (originalCount == 1) {
					this.setItem(slot, stack);
				}
			}
		}
		return false;
	}

	private boolean isFull() {
		for (ItemStack stack : this.items) {
			if (stack.isEmpty() || stack.getCount() != stack.getMaxStackSize()) {
				return false;
			}
		}
		return true;
	}

	private boolean isOnCooldown() {
		return this.cooldownTime > 0;
	}
}
