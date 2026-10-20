package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import tr.gokhan.easystorage.menu.FilterHopperMenu;

/**
 * Placeholder screen drawn with flat fills; final art will replace it with a texture.
 */
public class FilterHopperScreen<T extends FilterHopperMenu> extends AbstractContainerScreen<T> {
	private static final int WIDTH = 176;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_DARK = 0xFF555555;
	private static final int SLOT_BG = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int FILTER_TINT = 0xFF3A6EA5;
	private static final int ARROW = 0xFF555555;

	public FilterHopperScreen(T menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, menu.screenHeight());
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;

		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
		graphics.fill(x, y, x + this.imageWidth - 1, y + this.imageHeight - 1, PANEL_LIGHT);
		graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, PANEL);

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
			int ay = y + FilterHopperMenu.SLOT_Y + 7;
			graphics.fill(x + 40, ay, x + 54, ay + 2, ARROW);
			graphics.fill(x + 54, ay - 2, x + 56, ay + 4, ARROW);
		}

		for (Slot slot : this.menu.slots) {
			int sx = x + slot.x;
			int sy = y + slot.y;
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, PANEL_LIGHT);
			graphics.fill(sx - 1, sy - 1, sx + 16, sy + 16, SLOT_DARK);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
		}
	}
}
