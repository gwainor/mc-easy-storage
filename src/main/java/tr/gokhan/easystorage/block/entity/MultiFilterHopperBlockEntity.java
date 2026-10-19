package tr.gokhan.easystorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import tr.gokhan.easystorage.menu.MultiFilterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/** Filter Hopper with {@value #FILTER_SIZE} filter slots; an item passes if it matches any of them. */
public class MultiFilterHopperBlockEntity extends FilterHopperBlockEntity {
	public static final int FILTER_SIZE = 27;
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.multi_filter_hopper");

	public MultiFilterHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.MULTI_FILTER_HOPPER, pos, state, FILTER_SIZE);
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new MultiFilterHopperMenu(containerId, inventory, this, this.getFilter());
	}
}
