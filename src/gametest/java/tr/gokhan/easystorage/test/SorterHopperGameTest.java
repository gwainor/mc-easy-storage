package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

public class SorterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos SORTER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	@GameTest
	public void comparatorIgnoresFilterSlot(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.DIAMOND));
		helper.assertValueEqual(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter), 0, "signal with filter only");

		sorter.setItem(0, new ItemStack(Items.DIAMOND));
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter) > 0, "signal with buffered item");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsBufferAndFilter(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.EMERALD));
		sorter.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.getLevel().destroyBlock(helper.absolutePos(SORTER), true);

		helper.assertItemEntityPresent(Items.DIAMOND, SORTER, 2.0);
		helper.assertItemEntityPresent(Items.EMERALD, SORTER, 2.0);
		helper.succeed();
	}

	@GameTest
	public void shiftClickFillsFilterThenBuffer(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		SorterHopperMenu menu = new SorterHopperMenu(0, player.getInventory(), sorter, sorter.getFilter());
		// Menu slots: 0 = filter, 1-5 = buffer, 6-32 = main inventory, 33-41 = hotbar.
		int diamondSlot = SorterHopperMenu.BUFFER_END;
		int dirtSlot = SorterHopperMenu.BUFFER_END + 1;
		menu.getSlot(diamondSlot).set(new ItemStack(Items.DIAMOND, 5));
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 3));

		menu.quickMoveStack(player, diamondSlot);
		helper.assertTrue(sorter.getFilterItem().is(Items.DIAMOND), "first shift-click sets filter");
		helper.assertValueEqual(sorter.getFilterItem().getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(diamondSlot).getItem().getCount(), 4, "diamonds left in inventory");

		menu.quickMoveStack(player, diamondSlot);
		helper.assertValueEqual(sorter.countItem(Items.DIAMOND), 4, "diamonds in buffer");

		menu.quickMoveStack(player, dirtSlot);
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 3, "dirt stays in inventory");
		helper.assertValueEqual(sorter.countItem(Items.DIRT), 0, "dirt in buffer");
		helper.succeed();
	}
}
