package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.AABB;
import tr.gokhan.easystorage.block.FilterHopperBlock;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;
import tr.gokhan.easystorage.menu.FilterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

import java.util.List;

public class FilterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos HOPPER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	/** Chest above, filter hopper (facing down) in the middle, chest below. */
	private static FilterHopperBlockEntity column(GameTestHelper helper, ItemStack filter) {
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		FilterHopperBlockEntity filterHopper = helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class);
		filterHopper.setFilterItem(filter);
		return filterHopper;
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
		//                 filter hopper below line hopper -> sorted chest
		BlockPos source = new BlockPos(1, 4, 1);
		BlockPos lineHopper = new BlockPos(1, 3, 1);
		BlockPos nextHopper = new BlockPos(2, 3, 1);
		BlockPos overflow = new BlockPos(2, 2, 1);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(lineHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(nextHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(overflow, Blocks.CHEST);
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class).setFilterItem(new ItemStack(Items.DIAMOND));

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
			helper.assertContainerEmpty(HOPPER);
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
			helper.assertBlockProperty(HOPPER, FilterHopperBlock.ENABLED, false);
			helper.assertContainerEmpty(HOPPER);
			helper.assertContainerEmpty(BOTTOM);
			helper.assertContainerContains(TOP, Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest
	public void comparatorIgnoresFilterSlot(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		FilterHopperBlockEntity filterHopper = helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class);
		filterHopper.setFilterItem(new ItemStack(Items.DIAMOND));
		helper.assertValueEqual(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(filterHopper), 0, "signal with filter only");

		filterHopper.setItem(0, new ItemStack(Items.DIAMOND));
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(filterHopper) > 0, "signal with buffered item");
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
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		FilterHopperBlockEntity filterHopper = helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class);
		filterHopper.setFilterItem(new ItemStack(Items.EMERALD));
		filterHopper.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.getLevel().destroyBlock(helper.absolutePos(HOPPER), true);

		List<ItemEntity> dropped = helper.getLevel()
			.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(HOPPER)).inflate(2.0));
		int diamondCount = dropped.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
		int emeraldCount = dropped.stream().filter(e -> e.getItem().is(Items.EMERALD)).mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertValueEqual(diamondCount, 3, "diamonds dropped");
		helper.assertValueEqual(emeraldCount, 1, "emeralds dropped");
		helper.succeed();
	}

	@GameTest(maxTicks = 30)
	public void hopperCannotFillFilterOrPushNonMatching(GameTestHelper helper) {
		helper.setBlock(TOP, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		FilterHopperBlockEntity filterHopper = helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class);
		filterHopper.setFilterItem(new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIRT));

		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(HOPPER);
			helper.assertTrue(filterHopper.getFilterItem().is(Items.DIAMOND), "filter must be unchanged");
			helper.assertContainerContains(TOP, Items.DIRT);
			helper.succeed();
		});
	}

	@GameTest
	public void shiftClickFillsFilterThenBuffer(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		FilterHopperBlockEntity filterHopper = helper.getBlockEntity(HOPPER, FilterHopperBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		FilterHopperMenu menu = new FilterHopperMenu(0, player.getInventory(), filterHopper, filterHopper.getFilter());
		// Menu slots: 0 = filter, 1-5 = buffer, 6-32 = main inventory, 33-41 = hotbar.
		int diamondSlot = FilterHopperMenu.BUFFER_END;
		int dirtSlot = FilterHopperMenu.BUFFER_END + 1;
		menu.getSlot(diamondSlot).set(new ItemStack(Items.DIAMOND, 5));
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 3));

		menu.clicked(diamondSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(filterHopper.getFilterItem().is(Items.DIAMOND), "first shift-click sets filter");
		helper.assertValueEqual(filterHopper.getFilterItem().getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(diamondSlot).getItem().getCount(), 4, "diamonds left in inventory");
		helper.assertValueEqual(filterHopper.countItem(Items.DIAMOND), 0, "buffer must stay empty on first shift-click");

		menu.clicked(diamondSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(filterHopper.countItem(Items.DIAMOND), 4, "diamonds in buffer");

		menu.clicked(dirtSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 3, "dirt stays in inventory");
		helper.assertValueEqual(filterHopper.countItem(Items.DIRT), 0, "dirt in buffer");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsFilterHopperItem(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.FILTER_HOPPER);
		helper.getLevel().destroyBlock(helper.absolutePos(HOPPER), true);
		helper.assertItemEntityPresent(ModBlocks.FILTER_HOPPER.asItem(), HOPPER, 2.0);
		helper.succeed();
	}
}
