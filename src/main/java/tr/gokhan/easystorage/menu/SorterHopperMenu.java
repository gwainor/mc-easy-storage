package tr.gokhan.easystorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModMenus;

public class SorterHopperMenu extends AbstractContainerMenu {
	public static final int FILTER_SLOT = 0;
	public static final int BUFFER_START = 1;
	public static final int BUFFER_END = BUFFER_START + SorterHopperBlockEntity.BUFFER_SIZE;
	public static final int FILTER_X = 17;
	public static final int BUFFER_X = 62;
	public static final int SLOT_Y = 20;

	private final Container buffer;
	private final Container filter;

	/** Client-side constructor. */
	public SorterHopperMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(SorterHopperBlockEntity.BUFFER_SIZE), new SimpleContainer(1));
	}

	public SorterHopperMenu(int containerId, Inventory inventory, Container buffer, Container filter) {
		super(ModMenus.SORTER_HOPPER, containerId);
		checkContainerSize(buffer, SorterHopperBlockEntity.BUFFER_SIZE);
		checkContainerSize(filter, 1);
		this.buffer = buffer;
		this.filter = filter;
		buffer.startOpen(inventory.player);

		this.addSlot(new FilterSlot(filter, 0, FILTER_X, SLOT_Y));
		for (int i = 0; i < SorterHopperBlockEntity.BUFFER_SIZE; i++) {
			this.addSlot(new BufferSlot(buffer, i, BUFFER_X + i * 18, SLOT_Y));
		}
		this.addStandardInventorySlots(inventory, 8, 51);
	}

	private ItemStack filterItem() {
		return this.filter.getItem(0);
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
		if (slotIndex < BUFFER_END) {
			if (!this.moveItemStackTo(stack, BUFFER_END, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.filterItem().isEmpty()) {
			if (!this.moveItemStackTo(stack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (SorterHopperBlockEntity.matchesFilter(this.filterItem(), stack)) {
			if (!this.moveItemStackTo(stack, BUFFER_START, BUFFER_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
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
			return SorterHopperBlockEntity.matchesFilter(SorterHopperMenu.this.filterItem(), stack);
		}
	}
}
