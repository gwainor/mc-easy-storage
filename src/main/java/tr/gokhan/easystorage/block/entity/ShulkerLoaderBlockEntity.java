package tr.gokhan.easystorage.block.entity;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import tr.gokhan.easystorage.block.ShulkerLoaderBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import tr.gokhan.easystorage.menu.ShulkerLoaderMenu;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * Holds one shulker box in slot 0 and mirrors its contents in slots 1-27, so
 * automation can fill the box in place. The box item is the only persisted
 * state: contents are written back into its CONTAINER component on every change.
 */
public class ShulkerLoaderBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
	public static final int BOX_SLOT = 0;
	public static final int CONTENT_START = 1;
	public static final int CONTENT_SIZE = 27;
	public static final int CONTAINER_SIZE = 28;
	private static final int[] ALL_SLOTS = IntStream.range(0, CONTAINER_SIZE).toArray();
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.shulker_loader");

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private boolean pendingDrop;

	public ShulkerLoaderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SHULKER_LOADER, pos, state);
	}

	public static boolean isShulkerBox(ItemStack stack) {
		return stack.is(ItemTags.SHULKER_BOXES);
	}

	public boolean hasBox() {
		return !this.getBox().isEmpty();
	}

	public ItemStack getBox() {
		return this.items.get(BOX_SLOT);
	}

	public boolean isContentFull() {
		for (int slot = CONTENT_START; slot < CONTAINER_SIZE; slot++) {
			ItemStack stack = this.items.get(slot);
			if (stack.isEmpty() || stack.getCount() < this.getMaxStackSize(stack)) {
				return false;
			}
		}
		return true;
	}

	public boolean fits(ItemStack stack) {
		for (int slot = CONTENT_START; slot < CONTAINER_SIZE; slot++) {
			ItemStack current = this.items.get(slot);
			if (current.isEmpty() || (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < this.getMaxStackSize(current))) {
				return true;
			}
		}
		return false;
	}

	public void requestDrop() {
		if (this.hasBox()) {
			this.pendingDrop = true;
			this.setChanged();
		}
	}

	public boolean isDropPending() {
		return this.pendingDrop;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ShulkerLoaderBlockEntity loader) {
		if (!loader.hasBox()) {
			loader.pendingDrop = false;
			return;
		}
		// A pending drop fires even while powered; a fresh full-box drop needs the Loader unpowered.
		if ((loader.pendingDrop || (!state.getValue(ShulkerLoaderBlock.TRIGGERED) && loader.isContentFull())) && !loader.tryDrop(level, pos, state.getValue(ShulkerLoaderBlock.FACING))) {
			loader.pendingDrop = true;
		}
	}

	private boolean tryDrop(Level level, BlockPos pos, Direction facing) {
		this.writeContentsToBox();
		ItemStack box = this.getBox().copy();
		Container into = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
		if (into != null) {
			if (!HopperBlockEntity.addItem(this, into, box, facing.getOpposite()).isEmpty()) {
				return false;
			}
		} else {
			DefaultDispenseItemBehavior.spawnItem(level, box, 6, facing, Vec3.atCenterOf(pos).add(0.7 * facing.getStepX(), 0.7 * facing.getStepY(), 0.7 * facing.getStepZ()));
			level.levelEvent(1000, pos, 0);
			level.levelEvent(2000, pos, facing.get3DDataValue());
		}
		this.removeItem(BOX_SLOT, 1);
		this.pendingDrop = false;
		return true;
	}

	public int getAnalogSignal() {
		if (!this.hasBox()) {
			return 0;
		}
		float total = 0.0F;
		for (int slot = CONTENT_START; slot < CONTAINER_SIZE; slot++) {
			ItemStack stack = this.items.get(slot);
			if (!stack.isEmpty()) {
				total += (float) stack.getCount() / this.getMaxStackSize(stack);
			}
		}
		return Mth.lerpDiscrete(total / CONTENT_SIZE, 0, 15);
	}

	// --- Box <-> contents sync ---

	private void clearContents() {
		for (int slot = CONTENT_START; slot < CONTAINER_SIZE; slot++) {
			this.items.set(slot, ItemStack.EMPTY);
		}
	}

	private void loadContentsFromBox() {
		this.clearContents();
		NonNullList<ItemStack> contents = NonNullList.withSize(CONTENT_SIZE, ItemStack.EMPTY);
		this.getBox().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
		for (int i = 0; i < CONTENT_SIZE; i++) {
			this.items.set(CONTENT_START + i, contents.get(i));
		}
	}

	private void writeContentsToBox() {
		ItemStack box = this.getBox();
		if (!box.isEmpty()) {
			List<ItemStack> contents = this.items.subList(CONTENT_START, CONTAINER_SIZE).stream().map(ItemStack::copy).toList();
			box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		}
	}

	@Override
	public void setChanged() {
		this.writeContentsToBox();
		super.setChanged();
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot != BOX_SLOT) {
			super.setItem(slot, stack);
			return;
		}
		// Hoppers can leave the outgoing box stale (restored counts skip setChanged), so write it back first.
		this.writeContentsToBox();
		this.pendingDrop = false;
		this.items.set(BOX_SLOT, stack);
		stack.limitSize(this.getMaxStackSize(stack));
		this.loadContentsFromBox();
		super.setChanged();
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot != BOX_SLOT) {
			return super.removeItemNoUpdate(slot);
		}
		this.writeContentsToBox();
		this.pendingDrop = false;
		ItemStack box = this.items.set(BOX_SLOT, ItemStack.EMPTY);
		this.clearContents();
		return box;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		if (slot != BOX_SLOT) {
			return super.removeItem(slot, count);
		}
		ItemStack box = this.removeItemNoUpdate(BOX_SLOT);
		if (!box.isEmpty()) {
			super.setChanged();
		}
		return box;
	}

	// --- Container rules ---

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return isShulkerBox(stack) ? 1 : super.getMaxStackSize(stack);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == BOX_SLOT) {
			return isShulkerBox(stack) && stack.getCount() == 1 && !this.hasBox();
		}
		return this.hasBox() && stack.getItem().canFitInsideContainerItems();
	}

	@Override
	public int[] getSlotsForFace(Direction direction) {
		return ALL_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
		if (this.pendingDrop || this.getBlockState().getValue(ShulkerLoaderBlock.TRIGGERED) || !this.canPlaceItem(slot, stack)) {
			return false;
		}
		// Intentional side effect: refusing a non-fitting item also marks the box full (requestDrop).
		if (slot != BOX_SLOT && !this.fits(stack)) {
			this.requestDrop();
			return false;
		}
		return true;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
		return slot != BOX_SLOT;
	}

	// --- Persistence ---

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
		this.items.set(BOX_SLOT, input.read("box", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		this.loadContentsFromBox();
		this.pendingDrop = input.getBooleanOr("PendingDrop", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		this.writeContentsToBox();
		output.putBoolean("PendingDrop", this.pendingDrop);
		if (this.hasBox()) {
			output.store("box", ItemStack.CODEC, this.getBox());
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (this.level != null) {
			this.writeContentsToBox();
			Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), this.getBox());
		}
	}

	@Override
	public int getContainerSize() {
		return CONTAINER_SIZE;
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
		return new ShulkerLoaderMenu(containerId, inventory, this);
	}
}
