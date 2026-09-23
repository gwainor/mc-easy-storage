package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import tr.gokhan.easystorage.block.SorterHopperBlock;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

public class SorterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos SORTER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	/** Chest above, sorter (facing down) in the middle, chest below. */
	private static SorterHopperBlockEntity column(GameTestHelper helper, ItemStack filter) {
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(filter);
		return sorter;
	}

	private static Container container(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
	}

	@GameTest(maxTicks = 40)
	public void matchingItemIsPulledAndPushed(GameTestHelper helper) {
		column(helper, new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.succeedWhen(() -> helper.assertContainerContains(BOTTOM, Items.DIAMOND));
	}

	@GameTest(maxTicks = 60)
	public void nonMatchingItemContinuesAlongLine(GameTestHelper helper) {
		// source chest -> line hopper (east) -> next hopper (down) -> overflow chest
		//                 sorter below line hopper -> sorted chest
		BlockPos source = new BlockPos(1, 4, 1);
		BlockPos lineHopper = new BlockPos(1, 3, 1);
		BlockPos nextHopper = new BlockPos(2, 3, 1);
		BlockPos overflow = new BlockPos(2, 2, 1);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(lineHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(nextHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(overflow, Blocks.CHEST);
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class).setFilterItem(new ItemStack(Items.DIAMOND));

		container(helper, source).setItem(0, new ItemStack(Items.DIAMOND));
		container(helper, source).setItem(1, new ItemStack(Items.DIRT));

		helper.succeedWhen(() -> {
			helper.assertContainerContains(BOTTOM, Items.DIAMOND);
			helper.assertContainerContains(overflow, Items.DIRT);
			helper.assertFalse(container(helper, BOTTOM).countItem(Items.DIRT) > 0, "dirt must not be sorted");
			helper.assertFalse(container(helper, overflow).countItem(Items.DIAMOND) > 0, "diamond must be sorted");
		});
	}

	@GameTest(maxTicks = 30)
	public void emptyFilterAcceptsNothing(GameTestHelper helper) {
		column(helper, ItemStack.EMPTY);
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(SORTER);
			helper.assertContainerEmpty(BOTTOM);
			helper.assertContainerContains(TOP, Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void redstoneSignalLocksTransfers(GameTestHelper helper) {
		column(helper, new ItemStack(Items.DIAMOND));
		helper.setBlock(new BlockPos(2, 2, 1), Blocks.REDSTONE_BLOCK);
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(20, () -> {
			helper.assertBlockProperty(SORTER, SorterHopperBlock.ENABLED, false);
			helper.assertContainerEmpty(SORTER);
			helper.assertContainerEmpty(BOTTOM);
			helper.assertContainerContains(TOP, Items.DIAMOND);
			helper.succeed();
		});
	}

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

	@GameTest(maxTicks = 40)
	public void unstackableFilterIgnoresComponents(GameTestHelper helper) {
		column(helper, new ItemStack(Items.IRON_SWORD));
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(10);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 2);
		container(helper, TOP).setItem(0, sword);
		helper.succeedWhen(() -> helper.assertContainerContains(BOTTOM, Items.IRON_SWORD));
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

	@GameTest(maxTicks = 30)
	public void hopperCannotFillFilterOrPushNonMatching(GameTestHelper helper) {
		helper.setBlock(TOP, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIRT));

		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(SORTER);
			helper.assertTrue(sorter.getFilterItem().is(Items.DIAMOND), "filter must be unchanged");
			helper.assertContainerContains(TOP, Items.DIRT);
			helper.succeed();
		});
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

	@GameTest
	public void breakingDropsSorterHopperItem(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.getLevel().destroyBlock(helper.absolutePos(SORTER), true);
		helper.assertItemEntityPresent(ModBlocks.SORTER_HOPPER.asItem(), SORTER, 2.0);
		helper.succeed();
	}

}
