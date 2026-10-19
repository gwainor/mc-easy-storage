package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import tr.gokhan.easystorage.menu.ShulkerLoaderMenu;

/**
 * Placeholder screen drawn with flat fills; final art will replace it with a texture.
 */
public class ShulkerLoaderScreen extends AbstractContainerScreen<ShulkerLoaderMenu> {
	private static final int WIDTH = 176;
	private static final int HEIGHT = 200;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_DARK = 0xFF555555;
	private static final int SLOT_BG = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int BOX_TINT = 0xFF8A5AA5;
	private static final int LOCKED = 0xB0555555;

	public ShulkerLoaderScreen(ShulkerLoaderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
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

		// Tinted frame marks the box slot.
		int bx = x + ShulkerLoaderMenu.BOX_X;
		int by = y + ShulkerLoaderMenu.BOX_Y;
		graphics.fill(bx - 3, by - 3, bx + 19, by + 19, BOX_TINT);

		for (Slot slot : this.menu.slots) {
			int sx = x + slot.x;
			int sy = y + slot.y;
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, PANEL_LIGHT);
			graphics.fill(sx - 1, sy - 1, sx + 16, sy + 16, SLOT_DARK);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
		}

		// Grey out the content grid until a box is loaded.
		if (!this.menu.hasBox()) {
			int gx = x + ShulkerLoaderMenu.GRID_X;
			int gy = y + ShulkerLoaderMenu.GRID_Y;
			graphics.fill(gx - 1, gy - 1, gx + 9 * 18 - 1, gy + 3 * 18 - 1, LOCKED);
		}
	}
}
