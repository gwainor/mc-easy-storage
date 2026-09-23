package tr.gokhan.easystorage.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import tr.gokhan.easystorage.block.ItemGateBlock;
import tr.gokhan.easystorage.block.entity.ItemGateBlockEntity;
import tr.gokhan.easystorage.block.entity.ShulkerUnloader;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModBlocks;

public class ItemGateGameTest {
	private static final BlockPos GATE = new BlockPos(1, 4, 1);
	private static final BlockPos BELOW = GATE.below();

	/** Places a gate whose output face points down and returns its block entity. */
	private static ItemGateBlockEntity gate(GameTestHelper helper) {
		helper.setBlock(GATE, ModBlocks.ITEM_GATE.defaultBlockState().setValue(ItemGateBlock.FACING, Direction.DOWN));
		return helper.getBlockEntity(GATE, ItemGateBlockEntity.class);
	}

	private static List<ItemEntity> droppedItems(GameTestHelper helper) {
		AABB area = new AABB(helper.absolutePos(GATE)).inflate(4.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
	}

	private static int droppedCount(GameTestHelper helper, Item item) {
		return droppedItems(helper).stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static ItemStack shulkerWith(ItemStack... contents) {
		ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
		shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(contents)));
		return shulker;
	}

	@GameTest(maxTicks = 20)
	public void dropsWholeStackOutOfOutputFace(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		gate.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
		helper.succeedWhen(() -> {
			List<ItemEntity> dropped = droppedItems(helper);
			helper.assertValueEqual(dropped.size(), 1, "dropped entities");
			helper.assertValueEqual(dropped.get(0).getItem().getCount(), 64, "dropped stack size");
			helper.assertTrue(dropped.get(0).getY() < helper.absolutePos(GATE).getY(), "item must leave through the bottom face");
			helper.assertTrue(gate.isEmpty(), "gate should be empty");
		});
	}

	@GameTest(maxTicks = 60)
	public void shulkerUnloadsThenDropsEmptyBox(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		ItemStack shulker = shulkerWith(new ItemStack(Items.DIAMOND, 5), ItemStack.EMPTY, new ItemStack(Items.DIRT, 3), new ItemStack(Items.IRON_INGOT, 2));
		shulker.set(DataComponents.CUSTOM_NAME, Component.literal("Loot"));
		gate.setItem(0, shulker);

		helper.succeedWhen(() -> {
			helper.assertTrue(gate.isEmpty(), "gate should be empty");
			helper.assertValueEqual(droppedCount(helper, Items.DIAMOND), 5, "diamonds dropped");
			helper.assertValueEqual(droppedCount(helper, Items.DIRT), 3, "dirt dropped");
			helper.assertValueEqual(droppedCount(helper, Items.IRON_INGOT), 2, "iron dropped");
			ItemStack box = droppedItems(helper).stream().map(ItemEntity::getItem).filter(s -> s.is(Items.SHULKER_BOX)).findFirst().orElse(ItemStack.EMPTY);
			helper.assertFalse(box.isEmpty(), "empty shulker box must be dropped");
			helper.assertFalse(ShulkerUnloader.hasContents(box), "dropped shulker box must be empty");
			helper.assertValueEqual(box.getHoverName().getString(), "Loot", "shulker custom name");
		});
	}

	@GameTest(maxTicks = 60)
	public void dropsAreEightTicksApart(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		gate.setItem(0, new ItemStack(Items.DIRT));
		gate.setItem(1, new ItemStack(Items.SAND));
		gate.setItem(2, new ItemStack(Items.GRAVEL));
		List<Long> dropTicks = new ArrayList<>();
		int[] lastCount = {0};
		helper.onEachTick(() -> {
			int count = droppedItems(helper).size();
			if (count > lastCount[0]) {
				dropTicks.add(helper.getTick());
				lastCount[0] = count;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertValueEqual(dropTicks.size(), 3, "number of drops");
			helper.assertValueEqual(dropTicks.get(1) - dropTicks.get(0), 8L, "ticks between drop 1 and 2");
			helper.assertValueEqual(dropTicks.get(2) - dropTicks.get(1), 8L, "ticks between drop 2 and 3");
		});
	}

	@GameTest(maxTicks = 30)
	public void blockedOutputFaceDropsNothing(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		helper.setBlock(BELOW, Blocks.STONE);
		gate.setItem(0, new ItemStack(Items.DIRT, 10));
		helper.runAfterDelay(20, () -> {
			helper.assertValueEqual(droppedItems(helper).size(), 0, "dropped entities");
			helper.assertValueEqual(gate.countItem(Items.DIRT), 10, "dirt still in gate");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void redstoneSignalLocksGate(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		helper.setBlock(GATE.east(), Blocks.REDSTONE_BLOCK);
		gate.setItem(0, new ItemStack(Items.DIRT, 10));
		helper.runAfterDelay(20, () -> {
			helper.assertBlockProperty(GATE, ItemGateBlock.ENABLED, false);
			helper.assertValueEqual(droppedItems(helper).size(), 0, "dropped entities");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
	public void hopperFeedsGateAndComparatorReadsIt(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		helper.setBlock(BELOW, Blocks.STONE);
		BlockPos hopper = GATE.above();
		helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.getBlockEntity(hopper, HopperBlockEntity.class).setItem(0, new ItemStack(Items.DIRT, 3));
		helper.succeedWhen(() -> {
			helper.assertValueEqual(gate.countItem(Items.DIRT), 3, "dirt moved into gate");
			helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(gate) > 0, "comparator signal");
		});
	}

	@GameTest
	public void breakingDropsContentsIncludingLoadedShulker(GameTestHelper helper) {
		ItemGateBlockEntity gate = gate(helper);
		helper.setBlock(BELOW, Blocks.STONE);
		gate.setItem(0, shulkerWith(new ItemStack(Items.DIAMOND, 4)));
		gate.setItem(1, new ItemStack(Items.DIRT, 7));

		helper.getLevel().destroyBlock(helper.absolutePos(GATE), true);

		helper.assertValueEqual(droppedCount(helper, Items.DIRT), 7, "dirt dropped");
		ItemStack box = droppedItems(helper).stream().map(ItemEntity::getItem).filter(s -> s.is(Items.SHULKER_BOX)).findFirst().orElse(ItemStack.EMPTY);
		helper.assertTrue(ShulkerUnloader.hasContents(box), "shulker box keeps its remaining contents");
		helper.succeed();
	}

	@GameTest(maxTicks = 100)
	public void feedsSorterHopperThroughTheAir(GameTestHelper helper) {
		BlockPos sorterPos = new BlockPos(1, 2, 1);
		BlockPos chest = new BlockPos(1, 1, 1);
		ItemGateBlockEntity gate = gate(helper);
		helper.setBlock(sorterPos, ModBlocks.SORTER_HOPPER);
		helper.setBlock(chest, Blocks.CHEST);
		helper.getBlockEntity(sorterPos, SorterHopperBlockEntity.class).setFilterItem(new ItemStack(Items.DIAMOND));
		gate.setItem(0, new ItemStack(Items.DIAMOND, 4));
		gate.setItem(1, new ItemStack(Items.DIRT, 2));

		helper.succeedWhen(() -> {
			helper.assertValueEqual(helper.getBlockEntity(chest, ChestBlockEntity.class).countItem(Items.DIAMOND), 4, "diamonds sorted");
			helper.assertValueEqual(droppedCount(helper, Items.DIRT), 2, "dirt left floating");
		});
	}

	@GameTest
	public void shulkerUnloaderTakesStacksInOrder(GameTestHelper helper) {
		ItemStack shulker = shulkerWith(ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 2), ItemStack.EMPTY, new ItemStack(Items.DIRT, 5));
		helper.assertTrue(ShulkerUnloader.hasContents(shulker), "full shulker has contents");
		helper.assertTrue(ShulkerUnloader.takeNext(shulker).is(Items.DIAMOND), "first taken stack");
		helper.assertTrue(ShulkerUnloader.hasContents(shulker), "one stack left");
		ItemStack second = ShulkerUnloader.takeNext(shulker);
		helper.assertTrue(second.is(Items.DIRT) && second.getCount() == 5, "second taken stack");
		helper.assertFalse(ShulkerUnloader.hasContents(shulker), "shulker now empty");
		helper.assertTrue(ShulkerUnloader.takeNext(shulker).isEmpty(), "empty shulker yields EMPTY");
		helper.assertFalse(ShulkerUnloader.hasContents(new ItemStack(Items.CHEST)), "non-shulker never has contents");

		ItemStack dyed = new ItemStack(Items.DYED_SHULKER_BOX.red());
		dyed.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.STONE, 9))));
		helper.assertTrue(ShulkerUnloader.hasContents(dyed), "dyed shulker box is unloaded too");
		helper.assertValueEqual(ShulkerUnloader.takeNext(dyed).getCount(), 9, "stone taken from dyed shulker");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsItemGateItem(GameTestHelper helper) {
		gate(helper);
		helper.getLevel().destroyBlock(helper.absolutePos(GATE), true);
		helper.assertItemEntityPresent(ModBlocks.ITEM_GATE.asItem(), GATE, 2.0);
		helper.succeed();
	}
}
