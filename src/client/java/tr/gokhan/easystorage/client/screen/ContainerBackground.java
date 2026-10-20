package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Builds screen backgrounds from pieces of vanilla's chest texture, so resource packs
 * that restyle containers (e.g. dark mode) restyle ours too.
 */
final class ContainerBackground {
	private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
	private static final Identifier FURNACE = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
	private static final int WIDTH = 176;
	private static final int STRIP_V = 4;
	private static final int STRIP_HEIGHT = 13;
	private static final int INVENTORY_V = 126;
	private static final int INVENTORY_HEIGHT = 96;
	// Player slots sit this far below the top of the texture's inventory section.
	private static final int INVENTORY_SLOT_OFFSET = 14;

	private ContainerBackground() {
	}

	/** Panel with border and the player inventory; the menu's own slots are drawn by {@link #extractSlots}. */
	static void extractPanel(GuiGraphicsExtractor graphics, int x, int y, AbstractContainerMenu menu) {
		int inventoryTop = y + menu.slots.stream().filter(s -> s.container instanceof Inventory).mapToInt(s -> s.y).min().orElseThrow() - INVENTORY_SLOT_OFFSET;
		blit(graphics, CHEST, x, y, 0, 0, WIDTH, STRIP_V);
		// The plain strip under the chest title, repeated down to the inventory section.
		for (int top = y + STRIP_V; top < inventoryTop; top += STRIP_HEIGHT) {
			blit(graphics, CHEST, x, top, 0, STRIP_V, WIDTH, Math.min(STRIP_HEIGHT, inventoryTop - top));
		}
		blit(graphics, CHEST, x, inventoryTop, 0, INVENTORY_V, WIDTH, INVENTORY_HEIGHT);
	}

	static void extractSlots(GuiGraphicsExtractor graphics, int x, int y, AbstractContainerMenu menu) {
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof Inventory)) {
				blit(graphics, CHEST, x + slot.x - 1, y + slot.y - 1, 7, 17, 18, 18);
			}
		}
	}

	/** The furnace's empty progress arrow, 24x17. */
	static void extractArrow(GuiGraphicsExtractor graphics, int x, int y) {
		blit(graphics, FURNACE, x, y, 79, 34, 24, 17);
	}

	private static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int u, int v, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, 256, 256);
	}
}
