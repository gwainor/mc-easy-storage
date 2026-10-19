package tr.gokhan.easystorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModMenus;

public class FilterHopperMenu extends AbstractContainerMenu {
	public static final int FILTER_SLOT = 0;
	public static final int BUFFER_START = 1;
	public static final int BUFFER_END = BUFFER_START + FilterHopperBlockEntity.BUFFER_SIZE;
	public static final int FILTER_X = 17;
	public static final int BUFFER_X = 62;
	public static final int SLOT_Y = 20;
	private static final int INVENTORY_Y = 51;
	private static final int SCREEN_HEIGHT = 133;

	private final Container buffer;
	private final Container filter;
	private final int filterSize;
	private final int bufferStart;
	private final int bufferEnd;

	/** Client-side constructor. */
	public FilterHopperMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(FilterHopperBlockEntity.BUFFER_SIZE), new SimpleContainer(1));
	}

	public FilterHopperMenu(int containerId, Inventory inventory, Container buffer, Container filter) {
		this(ModMenus.FILTER_HOPPER, containerId, inventory, buffer, filter, FILTER_X, SLOT_Y, 1, BUFFER_X, SLOT_Y, INVENTORY_Y);
		checkContainerSize(filter, 1);
	}

	protected FilterHopperMenu(MenuType<?> type, int containerId, Inventory inventory, Container buffer, Container filter,
		int filterX, int filterY, int filterColumns, int bufferX, int bufferY, int inventoryY) {
		super(type, containerId);
		checkContainerSize(buffer, FilterHopperBlockEntity.BUFFER_SIZE);
		this.filterSize = filter.getContainerSize();
		this.bufferStart = this.filterSize;
		this.bufferEnd = this.bufferStart + FilterHopperBlockEntity.BUFFER_SIZE;
		this.buffer = buffer;
		this.filter = filter;
		buffer.startOpen(inventory.player);

		for (int i = 0; i < this.filterSize; i++) {
			this.addSlot(new FilterSlot(filter, i, filterX + i % filterColumns * 18, filterY + i / filterColumns * 18));
		}
		for (int i = 0; i < FilterHopperBlockEntity.BUFFER_SIZE; i++) {
			this.addSlot(new BufferSlot(buffer, i, bufferX + i * 18, bufferY));
		}
		this.addStandardInventorySlots(inventory, 8, inventoryY);
	}

	public int filterSize() {
		return this.filterSize;
	}

	public boolean isFilterSlot(Slot slot) {
		return slot.container == this.filter;
	}

	public int screenHeight() {
		return SCREEN_HEIGHT;
	}

	@Override
	public boolean stillValid(Player player) {
		return this.buffer.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < this.bufferEnd) {
			if (!this.moveItemStackTo(stack, this.bufferEnd, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (FilterHopperBlockEntity.matchesFilter(this.filter, stack)) {
			if (!this.moveItemStackTo(stack, this.bufferStart, this.bufferEnd, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			int empty = this.firstEmptyFilterSlot();
			if (empty < 0) {
				return ItemStack.EMPTY;
			}
			// Only one item ever goes into a filter slot. Report EMPTY (rather than the
			// remaining stack) so AbstractContainerMenu#clicked stops looping instead of
			// re-invoking us and dumping the rest of the stack into the now-filled filter's buffer.
			this.filter.setItem(empty, stack.split(1));
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	private int firstEmptyFilterSlot() {
		for (int i = 0; i < this.filterSize; i++) {
			if (this.filter.getItem(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		if (this.isFilterSlot(target)) {
			return false;
		}
		return super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.buffer.stopOpen(player);
	}

	private static class FilterSlot extends Slot {
		FilterSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	private class BufferSlot extends Slot {
		BufferSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return FilterHopperBlockEntity.matchesFilter(FilterHopperMenu.this.filter, stack);
		}
	}
}
