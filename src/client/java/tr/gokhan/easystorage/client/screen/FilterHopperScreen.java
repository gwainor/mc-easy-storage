package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import tr.gokhan.easystorage.menu.FilterHopperMenu;

public class FilterHopperScreen<T extends FilterHopperMenu> extends AbstractContainerScreen<T> {
	private static final int WIDTH = 176;
	private static final int FILTER_TINT = 0xFF3A6EA5;

	public FilterHopperScreen(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, menu.screenHeight());
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		ContainerBackground.extractPanel(graphics, x, y, this.menu);

		// Tinted frame marks each filter slot.
		for (Slot slot : this.menu.slots) {
			if (this.menu.isFilterSlot(slot)) {
				int fx = x + slot.x;
				int fy = y + slot.y;
				graphics.fill(fx - 3, fy - 3, fx + 19, fy + 19, FILTER_TINT);
			}
		}

		// Arrow from filter to buffer (single-filter layout only).
		if (this.menu.filterSize() == 1) {
			ContainerBackground.extractArrow(graphics, x + 37, y + FilterHopperMenu.SLOT_Y);
		}

		ContainerBackground.extractSlots(graphics, x, y, this.menu);
	}
}
