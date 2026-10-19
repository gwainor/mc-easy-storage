package tr.gokhan.easystorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tr.gokhan.easystorage.block.entity.ShulkerLoaderBlockEntity;
import tr.gokhan.easystorage.registry.ModMenus;

public class ShulkerLoaderMenu extends AbstractContainerMenu {
	public static final int BOX_SLOT = ShulkerLoaderBlockEntity.BOX_SLOT;
	public static final int CONTENT_START = ShulkerLoaderBlockEntity.CONTENT_START;
	public static final int CONTENT_END = CONTENT_START + ShulkerLoaderBlockEntity.CONTENT_SIZE;
	public static final int BOX_X = 80;
	public static final int BOX_Y = 20;
	public static final int GRID_X = 8;
	public static final int GRID_Y = 46;

	private final Container loader;

	/** Client-side constructor. */
	public ShulkerLoaderMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(ShulkerLoaderBlockEntity.CONTAINER_SIZE));
	}

	public ShulkerLoaderMenu(int containerId, Inventory inventory, Container loader) {
		super(ModMenus.SHULKER_LOADER, containerId);
		checkContainerSize(loader, ShulkerLoaderBlockEntity.CONTAINER_SIZE);
		this.loader = loader;
		loader.startOpen(inventory.player);

		this.addSlot(new BoxSlot(loader, BOX_SLOT, BOX_X, BOX_Y));
		for (int i = 0; i < ShulkerLoaderBlockEntity.CONTENT_SIZE; i++) {
			this.addSlot(new ContentSlot(loader, CONTENT_START + i, GRID_X + i % 9 * 18, GRID_Y + i / 9 * 18));
		}
		this.addStandardInventorySlots(inventory, 8, 118);
	}

	public boolean hasBox() {
		return !this.loader.getItem(BOX_SLOT).isEmpty();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.loader.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < CONTENT_END) {
			// Sync the box's CONTAINER component before the stack leaves, so contents travel with it.
			slot.setChanged();
			original = stack.copy();
			if (!this.moveItemStackTo(stack, CONTENT_END, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (ShulkerLoaderBlockEntity.isShulkerBox(stack) && !this.hasBox()) {
			if (!this.moveItemStackTo(stack, BOX_SLOT, BOX_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
			// Only one box ever loads; report EMPTY so clicked() stops instead of looping on the rest.
			if (stack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
			return ItemStack.EMPTY;
		} else if (this.hasBox()) {
			if (!this.moveItemStackTo(stack, CONTENT_START, CONTENT_END, false)) {
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
	public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
		if (target.index == BOX_SLOT) {
			return false;
		}
		return super.canTakeItemForPickAll(carried, target);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.loader.stopOpen(player);
	}

	private static class BoxSlot extends Slot {
		BoxSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return ShulkerLoaderBlockEntity.isShulkerBox(stack);
		}
	}

	private class ContentSlot extends Slot {
		ContentSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return ShulkerLoaderMenu.this.hasBox() && stack.getItem().canFitInsideContainerItems();
		}
	}
}
