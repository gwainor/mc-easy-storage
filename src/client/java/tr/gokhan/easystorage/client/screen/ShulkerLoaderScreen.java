package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import tr.gokhan.easystorage.menu.ShulkerLoaderMenu;

public class ShulkerLoaderScreen extends AbstractContainerScreen<ShulkerLoaderMenu> {
	private static final int WIDTH = 176;
	private static final int HEIGHT = 200;
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
		ContainerBackground.extractPanel(graphics, x, y, this.menu);

		// Tinted frame marks the box slot.
		int bx = x + ShulkerLoaderMenu.BOX_X;
		int by = y + ShulkerLoaderMenu.BOX_Y;
		graphics.fill(bx - 3, by - 3, bx + 19, by + 19, BOX_TINT);

		ContainerBackground.extractSlots(graphics, x, y, this.menu);

		// Grey out the content grid until a box is loaded.
		if (!this.menu.hasBox()) {
			int gx = x + ShulkerLoaderMenu.GRID_X;
			int gy = y + ShulkerLoaderMenu.GRID_Y;
			graphics.fill(gx - 1, gy - 1, gx + 9 * 18 - 1, gy + 3 * 18 - 1, LOCKED);
		}
	}
}
