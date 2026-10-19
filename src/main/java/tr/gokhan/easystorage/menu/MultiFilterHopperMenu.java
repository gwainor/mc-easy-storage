package tr.gokhan.easystorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;
import tr.gokhan.easystorage.block.entity.MultiFilterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModMenus;

public class MultiFilterHopperMenu extends FilterHopperMenu {
	public static final int BUFFER_START = MultiFilterHopperBlockEntity.FILTER_SIZE;
	public static final int BUFFER_END = BUFFER_START + FilterHopperBlockEntity.BUFFER_SIZE;
	private static final int SCREEN_HEIGHT = 193;

	/** Client-side constructor. */
	public MultiFilterHopperMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(FilterHopperBlockEntity.BUFFER_SIZE), new SimpleContainer(MultiFilterHopperBlockEntity.FILTER_SIZE));
	}

	public MultiFilterHopperMenu(int containerId, Inventory inventory, Container buffer, Container filter) {
		super(ModMenus.MULTI_FILTER_HOPPER, containerId, inventory, buffer, filter, 8, 18, 9, 44, 80, 111);
		checkContainerSize(filter, MultiFilterHopperBlockEntity.FILTER_SIZE);
	}

	@Override
	public int screenHeight() {
		return SCREEN_HEIGHT;
	}
}
