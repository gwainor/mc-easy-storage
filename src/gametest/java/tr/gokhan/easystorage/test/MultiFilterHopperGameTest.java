package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import tr.gokhan.easystorage.block.FilterHopperBlock;
import tr.gokhan.easystorage.block.entity.MultiFilterHopperBlockEntity;
import tr.gokhan.easystorage.menu.MultiFilterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

import java.util.List;

public class MultiFilterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos HOPPER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	/** Chest above, multi filter hopper (facing down) in the middle, chest below. */
	private static MultiFilterHopperBlockEntity column(GameTestHelper helper, ItemStack... filters) {
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		for (int i = 0; i < filters.length; i++) {
			hopper.setFilterItem(i, filters[i]);
		}
		return hopper;
	}

	private static Container container(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
	}

	@GameTest(maxTicks = 80)
	public void anyFilterItemIsAccepted(GameTestHelper helper) {
		MultiFilterHopperBlockEntity hopper = column(helper);
		hopper.setFilterItem(0, new ItemStack(Items.DIAMOND));
		hopper.setFilterItem(13, new ItemStack(Items.EMERALD));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(1, new ItemStack(Items.EMERALD));
		helper.succeedWhen(() -> {
			helper.assertContainerContains(BOTTOM, Items.DIAMOND);
			helper.assertContainerContains(BOTTOM, Items.EMERALD);
		});
	}

	@GameTest(maxTicks = 80)
	public void nonMatchingItemContinuesAlongLine(GameTestHelper helper) {
		BlockPos source = new BlockPos(1, 4, 1);
		BlockPos lineHopper = new BlockPos(1, 3, 1);
		BlockPos nextHopper = new BlockPos(2, 3, 1);
		BlockPos overflow = new BlockPos(2, 2, 1);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(lineHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(nextHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(overflow, Blocks.CHEST);
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		hopper.setFilterItem(0, new ItemStack(Items.DIAMOND));
		hopper.setFilterItem(1, new ItemStack(Items.EMERALD));

		container(helper, source).setItem(0, new ItemStack(Items.DIAMOND));
		container(helper, source).setItem(1, new ItemStack(Items.EMERALD));
		container(helper, source).setItem(2, new ItemStack(Items.DIRT));

		helper.succeedWhen(() -> {
			helper.assertContainerContains(BOTTOM, Items.DIAMOND);
			helper.assertContainerContains(BOTTOM, Items.EMERALD);
			helper.assertContainerContains(overflow, Items.DIRT);
			helper.assertFalse(container(helper, BOTTOM).countItem(Items.DIRT) > 0, "dirt must not be sorted");
		});
	}

	@GameTest(maxTicks = 30)
	public void emptyFilterAcceptsNothing(GameTestHelper helper) {
		column(helper);
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
	public void comparatorIgnoresFilters(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		for (int i = 0; i < MultiFilterHopperBlockEntity.FILTER_SIZE; i++) {
			hopper.setFilterItem(i, new ItemStack(Items.DIAMOND));
		}
		helper.assertValueEqual(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(hopper), 0, "signal with filters only");

		hopper.setItem(0, new ItemStack(Items.DIAMOND));
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(hopper) > 0, "signal with buffered item");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsBufferAndAllFilters(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		hopper.setFilterItem(0, new ItemStack(Items.DIAMOND));
		hopper.setFilterItem(26, new ItemStack(Items.EMERALD));
		hopper.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));

		helper.getLevel().destroyBlock(helper.absolutePos(HOPPER), true);

		List<ItemEntity> dropped = helper.getLevel()
			.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(HOPPER)).inflate(2.0));
		helper.assertValueEqual(count(dropped, Items.DIAMOND), 1, "diamonds dropped");
		helper.assertValueEqual(count(dropped, Items.EMERALD), 1, "emeralds dropped");
		helper.assertValueEqual(count(dropped, Items.GOLD_INGOT), 3, "gold dropped");
		helper.succeed();
	}

	private static int count(List<ItemEntity> dropped, net.minecraft.world.item.Item item) {
		return dropped.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
	}

	@GameTest(maxTicks = 30)
	public void hopperCannotFillFilters(GameTestHelper helper) {
		helper.setBlock(TOP, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		hopper.setFilterItem(0, new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIRT));

		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(HOPPER);
			Container filter = hopper.getFilter();
			helper.assertTrue(filter.getItem(0).is(Items.DIAMOND), "filter must be unchanged");
			helper.assertValueEqual(filter.countItem(Items.DIRT), 0, "no dirt in filters");
			helper.assertValueEqual(filter.countItem(Items.DIAMOND), 1, "only the diamond in filters");
			helper.succeed();
		});
	}

	@GameTest
	public void shiftClickStackAddsOneFilter(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		MultiFilterHopperMenu menu = new MultiFilterHopperMenu(0, player.getInventory(), hopper, hopper.getFilter());
		int dirtSlot = MultiFilterHopperMenu.BUFFER_END;
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 5));

		menu.clicked(dirtSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(hopper.getFilter().getItem(0).is(Items.DIRT), "first shift-click sets filter slot 0");
		helper.assertValueEqual(hopper.getFilter().getItem(0).getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 4, "dirt left in inventory");
		helper.assertValueEqual(hopper.countItem(Items.DIRT), 0, "buffer must stay empty on first shift-click");

		menu.clicked(dirtSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(hopper.countItem(Items.DIRT), 4, "dirt in buffer");
		helper.succeed();
	}

	@GameTest
	public void shiftClickWithFullFilterMovesNothing(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		MultiFilterHopperBlockEntity hopper = helper.getBlockEntity(HOPPER, MultiFilterHopperBlockEntity.class);
		for (int i = 0; i < MultiFilterHopperBlockEntity.FILTER_SIZE; i++) {
			hopper.setFilterItem(i, new ItemStack(Items.STONE));
		}
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		MultiFilterHopperMenu menu = new MultiFilterHopperMenu(0, player.getInventory(), hopper, hopper.getFilter());
		int dirtSlot = MultiFilterHopperMenu.BUFFER_END;
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 3));

		menu.clicked(dirtSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 3, "dirt stays in inventory");
		helper.assertValueEqual(hopper.countItem(Items.DIRT), 0, "dirt in buffer");
		helper.succeed();
	}

	@GameTest
	public void shiftClickUsesNextEmptyFilterSlot(GameTestHelper helper) {
		MultiFilterHopperBlockEntity hopper = column(helper, new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		MultiFilterHopperMenu menu = new MultiFilterHopperMenu(0, player.getInventory(), hopper, hopper.getFilter());
		int dirtSlot = MultiFilterHopperMenu.BUFFER_END;
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 5));

		menu.clicked(dirtSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(hopper.getFilter().getItem(2).is(Items.DIRT), "filter slot 2 holds dirt");
		helper.assertValueEqual(hopper.getFilter().getItem(2).getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 4, "dirt left in inventory");
		helper.succeed();
	}

	@GameTest
	public void saveLoadRoundTripKeepsAllFilters(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		MultiFilterHopperBlockEntity hopper = column(helper);
		hopper.setFilterItem(0, new ItemStack(Items.DIAMOND));
		hopper.setFilterItem(13, new ItemStack(Items.EMERALD));
		hopper.setFilterItem(26, new ItemStack(Items.GOLD_INGOT));
		CompoundTag tag = hopper.saveWithoutMetadata(registries);
		MultiFilterHopperBlockEntity fresh = new MultiFilterHopperBlockEntity(helper.absolutePos(HOPPER), hopper.getBlockState());
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
		helper.assertTrue(fresh.getFilter().getItem(0).is(Items.DIAMOND), "slot 0 kept");
		helper.assertTrue(fresh.getFilter().getItem(13).is(Items.EMERALD), "slot 13 kept");
		helper.assertTrue(fresh.getFilter().getItem(26).is(Items.GOLD_INGOT), "slot 26 kept");
		helper.assertTrue(fresh.getFilter().getItem(1).isEmpty(), "slot 1 empty");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsMultiFilterHopperItem(GameTestHelper helper) {
		helper.setBlock(HOPPER, ModBlocks.MULTI_FILTER_HOPPER);
		helper.getLevel().destroyBlock(helper.absolutePos(HOPPER), true);
		helper.assertItemEntityPresent(ModBlocks.MULTI_FILTER_HOPPER.asItem(), HOPPER, 2.0);
		helper.succeed();
	}
}
