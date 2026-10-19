package tr.gokhan.easystorage.test;

import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import tr.gokhan.easystorage.menu.ShulkerLoaderMenu;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import tr.gokhan.easystorage.block.ShulkerLoaderBlock;
import tr.gokhan.easystorage.block.entity.ShulkerLoaderBlockEntity;
import tr.gokhan.easystorage.registry.ModBlocks;

public class ShulkerLoaderGameTest {
	private static final BlockPos LOADER = new BlockPos(1, 2, 1);
	private static final BlockPos ABOVE = new BlockPos(1, 3, 1);

	private static ShulkerLoaderBlockEntity loader(GameTestHelper helper) {
		helper.setBlock(LOADER, ModBlocks.SHULKER_LOADER.defaultBlockState().setValue(ShulkerLoaderBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(LOADER, ShulkerLoaderBlockEntity.class);
	}

	private static BaseContainerBlockEntity hopperAbove(GameTestHelper helper) {
		helper.setBlock(ABOVE, Blocks.HOPPER);
		return helper.getBlockEntity(ABOVE, BaseContainerBlockEntity.class);
	}

	private static ItemStack boxWith(int slot, ItemStack stack) {
		ItemStack box = new ItemStack(Items.SHULKER_BOX);
		java.util.ArrayList<ItemStack> list = new java.util.ArrayList<>(java.util.Collections.nCopies(slot + 1, ItemStack.EMPTY));
		list.set(slot, stack);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
		return box;
	}

	private static int stored(ItemStack box, net.minecraft.world.item.Item item) {
		return box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
			.nonEmptyItemCopyStream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	@GameTest(maxTicks = 60)
	public void hopperLoadsOneBoxOnly(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		BaseContainerBlockEntity hopper = hopperAbove(helper);
		hopper.setItem(0, new ItemStack(Items.SHULKER_BOX));
		hopper.setItem(1, new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED)));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(loader.getBox().is(Items.SHULKER_BOX), "loader should hold the first box");
			helper.assertTrue(hopper.getItem(1).is(Items.DYED_SHULKER_BOX.pick(DyeColor.RED)), "hopper should keep the red box");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void stackedBoxesLoadOne(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		BaseContainerBlockEntity hopper = hopperAbove(helper);
		hopper.setItem(0, stackable(3));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(loader.getBox().is(Items.SHULKER_BOX) && loader.getBox().getCount() == 1, "loader should hold exactly one box");
			helper.assertTrue(hopper.getItem(0).getCount() == 2, "hopper should keep two boxes");
			helper.succeed();
		});
	}

	private static ItemStack stackable(int count) {
		ItemStack boxes = new ItemStack(Items.SHULKER_BOX, count);
		boxes.set(DataComponents.MAX_STACK_SIZE, 64);
		return boxes;
	}

	@GameTest
	public void multiCountBoxOfferIsRefused(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ItemStack rest = HopperBlockEntity.addItem(null, loader, stackable(3), Direction.UP);
		helper.assertTrue(rest.getCount() == 3, "all three boxes should be refused");
		helper.assertFalse(loader.hasBox(), "loader must stay empty");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void itemsRefusedWithoutBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		BaseContainerBlockEntity hopper = hopperAbove(helper);
		hopper.setItem(0, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(loader.isEmpty(), "loader must stay empty");
			helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND), "hopper keeps the diamond");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 80)
	public void itemsGoIntoLoadedBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		hopperAbove(helper).setItem(0, new ItemStack(Items.DIAMOND, 5));
		helper.succeedWhen(() -> {
			helper.assertTrue(loader.countItem(Items.DIAMOND) == 5, "loader should hold 5 diamonds");
			helper.assertTrue(stored(loader.getBox(), Items.DIAMOND) == 5, "box component should hold 5 diamonds");
		});
	}

	@GameTest
	public void boxNeverGoesIntoContents(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		helper.assertFalse(loader.canPlaceItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED))), "nested box");
		helper.succeed();
	}

	@GameTest
	public void prefilledBoxShowsContents(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(4, new ItemStack(Items.DIRT, 10)));
		ItemStack shown = loader.getItem(ShulkerLoaderBlockEntity.CONTENT_START + 4);
		helper.assertTrue(shown.is(Items.DIRT) && shown.getCount() == 10, "slot should show 10 dirt");
		helper.succeed();
	}

	@GameTest
	public void swappingBoxAfterFailedHopperPullKeepsContents(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		loader.setItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DIAMOND, 5));
		loader.setChanged();
		ItemStack box = loader.getBox();
		// Mimic a hopper that pulls one item, cannot place it, and restores the count without setChanged.
		ItemStack pulled = loader.removeItem(ShulkerLoaderBlockEntity.CONTENT_START, 1);
		loader.getItem(ShulkerLoaderBlockEntity.CONTENT_START).setCount(pulled.getCount() + 4);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, ItemStack.EMPTY);
		helper.assertTrue(stored(box, Items.DIAMOND) == 5, "swapped-out box should hold 5 diamonds, had " + stored(box, Items.DIAMOND));
		helper.succeed();
	}

	@GameTest
	public void replacingBoxClearsPendingDrop(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		loader.requestDrop();
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		helper.assertFalse(loader.isDropPending(), "pending drop should clear with the box");
		helper.succeed();
	}

	@GameTest
	public void takingBoxCarriesContentsAndClears(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		loader.setItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DIAMOND, 7));
		loader.setChanged();
		ItemStack taken = loader.removeItem(ShulkerLoaderBlockEntity.BOX_SLOT, 1);
		helper.assertTrue(taken.is(Items.SHULKER_BOX) && stored(taken, Items.DIAMOND) == 7, "box should carry 7 diamonds");
		helper.assertTrue(loader.isEmpty(), "loader should be empty");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsBoxWithContentsOnly(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		loader.setItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DIAMOND, 7));
		helper.destroyBlock(LOADER);
		List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(LOADER)).inflate(2.0));
		long boxes = drops.stream().filter(e -> e.getItem().is(Items.SHULKER_BOX)).count();
		helper.assertTrue(boxes == 1, "exactly one box should drop, got " + boxes);
		helper.assertTrue(drops.stream().filter(e -> e.getItem().is(Items.SHULKER_BOX)).allMatch(e -> stored(e.getItem(), Items.DIAMOND) == 7), "box keeps contents");
		helper.assertFalse(drops.stream().anyMatch(e -> e.getItem().is(Items.DIAMOND)), "no loose diamonds");
		helper.succeed();
	}

	@GameTest
	public void comparatorReadsContentsOnly(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		helper.assertTrue(loader.getAnalogSignal() == 0, "box alone reads 0");
		loader.setItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DIAMOND));
		helper.assertTrue(loader.getAnalogSignal() > 0, "contents read above 0");
		helper.succeed();
	}

	@GameTest
	public void saveLoadKeepsBoxContents(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		loader.setItem(ShulkerLoaderBlockEntity.CONTENT_START, new ItemStack(Items.DIAMOND, 7));
		CompoundTag tag = loader.saveWithoutMetadata(helper.getLevel().registryAccess());
		ShulkerLoaderBlockEntity fresh = new ShulkerLoaderBlockEntity(helper.absolutePos(LOADER), loader.getBlockState());
		fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
		helper.assertTrue(fresh.hasBox() && fresh.countItem(Items.DIAMOND) == 7, "reloaded loader keeps box and 7 diamonds");
		helper.succeed();
	}

	// --- Drops, redstone, lock ---

	private static final BlockPos FRONT = new BlockPos(2, 2, 1);
	private static final BlockPos POWER = new BlockPos(1, 2, 2);
	private static final BlockPos BELOW = new BlockPos(1, 1, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 0, 1);

	private static ChestBlockEntity chestAt(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.CHEST);
		return helper.getBlockEntity(pos, ChestBlockEntity.class);
	}

	private static ItemStack boxOf(ItemStack each) {
		ItemStack box = new ItemStack(Items.SHULKER_BOX);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Collections.nCopies(ShulkerLoaderBlockEntity.CONTENT_SIZE, each)));
		return box;
	}

	private static boolean holdsBox(BaseContainerBlockEntity container, net.minecraft.world.item.Item item, int amount) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (stack.is(Items.SHULKER_BOX) && stored(stack, item) == amount) {
				return true;
			}
		}
		return false;
	}

	private static void bottomChestLayout(GameTestHelper helper) {
		helper.setBlock(BELOW, Blocks.HOPPER);
		chestAt(helper, BOTTOM);
	}

	@GameTest(maxTicks = 60)
	public void fullBoxDropsIntoChestInFront(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ChestBlockEntity chest = chestAt(helper, FRONT);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxOf(new ItemStack(Items.COBBLESTONE, 64)));
		helper.succeedWhen(() -> {
			helper.assertTrue(holdsBox(chest, Items.COBBLESTONE, 1728), "chest should hold the full box");
			helper.assertFalse(loader.hasBox(), "loader should be empty");
		});
	}

	@GameTest(maxTicks = 60)
	public void fullBoxDropsAsItemEntity(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxOf(new ItemStack(Items.COBBLESTONE, 64)));
		helper.succeedWhen(() -> {
			List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FRONT)).inflate(2.0));
			helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(Items.SHULKER_BOX)), "box should drop as item");
			helper.assertFalse(loader.hasBox(), "loader should be empty");
		});
	}

	@GameTest(maxTicks = 100)
	public void itemThatDoesNotFitDropsBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ChestBlockEntity chest = chestAt(helper, FRONT);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxOf(new ItemStack(Items.DIRT)));
		BaseContainerBlockEntity hopper = hopperAbove(helper);
		hopper.setItem(0, new ItemStack(Items.DIAMOND));
		helper.startSequence()
			.thenWaitUntil(() -> {
				helper.assertTrue(holdsBox(chest, Items.DIRT, 27), "chest should hold the box");
				helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND), "hopper keeps the diamond");
			})
			.thenExecute(() -> loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX)))
			.thenWaitUntil(() -> helper.assertTrue(loader.countItem(Items.DIAMOND) == 1, "diamond should enter the fresh box"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 40)
	public void offeredBoxDoesNotTriggerDrop(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIRT)));
		hopperAbove(helper).setItem(0, new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED)));
		helper.runAfterDelay(20, () -> {
			helper.assertTrue(loader.hasBox(), "box stays loaded");
			helper.assertFalse(loader.isDropPending(), "no drop pending");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 60)
	public void redstonePulseDropsPartialBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ChestBlockEntity chest = chestAt(helper, FRONT);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND, 3)));
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		helper.succeedWhen(() -> helper.assertTrue(holdsBox(chest, Items.DIAMOND, 3), "chest should hold the partial box"));
	}

	@GameTest(maxTicks = 60)
	public void poweredLoaderRefusesInput(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		BaseContainerBlockEntity hopper = hopperAbove(helper);
		hopper.setItem(0, new ItemStack(Items.SHULKER_BOX));
		hopper.setItem(1, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(loader.isEmpty(), "powered loader stays empty");
			helper.assertTrue(helper.getBlockState(LOADER).getValue(ShulkerLoaderBlock.TRIGGERED), "loader is triggered");
			helper.assertTrue(hopper.getItem(0).is(Items.SHULKER_BOX) && hopper.getItem(1).is(Items.DIAMOND), "hopper keeps both");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void constantSignalDoesNotRefire(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		chestAt(helper, FRONT);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND)));
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertFalse(loader.hasBox(), "first box should drop"))
			.thenExecute(() -> loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND))))
			.thenIdle(20)
			.thenExecute(() -> helper.assertTrue(loader.hasBox(), "constant signal must not drop again"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void blockedDropStaysPending(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ChestBlockEntity chest = chestAt(helper, FRONT);
		for (int i = 0; i < chest.getContainerSize(); i++) {
			chest.setItem(i, new ItemStack(Items.STONE, 64));
		}
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND, 3)));
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		helper.startSequence()
			.thenIdle(10)
			.thenExecute(() -> helper.assertTrue(loader.isDropPending() && loader.hasBox(), "drop stays pending while blocked"))
			.thenExecute(() -> chest.setItem(0, ItemStack.EMPTY))
			.thenWaitUntil(() -> helper.assertTrue(holdsBox(chest, Items.DIAMOND, 3), "box drops once space frees"))
			.thenSucceed();
	}

	@GameTest(maxTicks = 60)
	public void hopperBelowPullsContentsNotBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		bottomChestLayout(helper);
		ChestBlockEntity bottom = helper.getBlockEntity(BOTTOM, ChestBlockEntity.class);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND, 3)));
		helper.succeedWhen(() -> {
			helper.assertTrue(bottom.countItem(Items.DIAMOND) == 3, "bottom chest should hold 3 diamonds");
			helper.assertTrue(loader.hasBox(), "box stays in loader");
			helper.assertTrue(bottom.countItem(Items.SHULKER_BOX) == 0, "no box in bottom chest");
		});
	}

	@GameTest(maxTicks = 80)
	public void hopperBelowWorksWhileLocked(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		bottomChestLayout(helper);
		ChestBlockEntity bottom = helper.getBlockEntity(BOTTOM, ChestBlockEntity.class);
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		helper.startSequence()
			.thenIdle(5)
			.thenExecute(() -> loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND, 3))))
			.thenWaitUntil(() -> {
				helper.assertTrue(bottom.countItem(Items.DIAMOND) == 3, "locked loader still gives contents");
				helper.assertTrue(loader.hasBox(), "box stays in loader");
			})
			.thenSucceed();
	}

	@GameTest(maxTicks = 100)
	public void saveLoadKeepsPendingDrop(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		ChestBlockEntity chest = chestAt(helper, FRONT);
		for (int i = 0; i < chest.getContainerSize(); i++) {
			chest.setItem(i, new ItemStack(Items.STONE, 64));
		}
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND)));
		helper.setBlock(POWER, Blocks.REDSTONE_BLOCK);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(loader.isDropPending(), "drop should be pending"))
			.thenExecute(() -> {
				CompoundTag tag = loader.saveWithoutMetadata(helper.getLevel().registryAccess());
				ShulkerLoaderBlockEntity fresh = new ShulkerLoaderBlockEntity(helper.absolutePos(LOADER), loader.getBlockState());
				fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
				helper.assertTrue(fresh.isDropPending(), "pending flag survives save/load");
			})
			.thenSucceed();
	}

	private static ItemStack stackableBox(DyeColor color, int count) {
		ItemStack box = new ItemStack(Items.DYED_SHULKER_BOX.pick(color), count);
		box.set(DataComponents.MAX_STACK_SIZE, 64);
		return box;
	}

	private static int inventoryCount(Player player, net.minecraft.world.item.Item item) {
		return player.getInventory().countItem(item);
	}

	@GameTest
	public void shiftClickLoadsOneBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ShulkerLoaderMenu menu = new ShulkerLoaderMenu(0, player.getInventory(), loader);
		int playerSlot = ShulkerLoaderMenu.CONTENT_END;
		menu.getSlot(playerSlot).set(stackableBox(DyeColor.RED, 3));

		menu.clicked(playerSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(loader.hasBox(), "box loaded");
		helper.assertValueEqual(loader.getBox().getCount(), 1, "loader holds exactly one box");
		helper.assertValueEqual(menu.getSlot(playerSlot).getItem().getCount(), 2, "player keeps the rest");
		helper.succeed();
	}

	@GameTest
	public void shiftClickItemsNeedBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ShulkerLoaderMenu menu = new ShulkerLoaderMenu(0, player.getInventory(), loader);
		int playerSlot = ShulkerLoaderMenu.CONTENT_END;
		menu.getSlot(playerSlot).set(new ItemStack(Items.DIAMOND, 5));

		menu.clicked(playerSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(loader.isEmpty(), "nothing moves without a box");
		helper.assertValueEqual(menu.getSlot(playerSlot).getItem().getCount(), 5, "player keeps diamonds");

		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		menu.clicked(playerSlot, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertValueEqual(loader.countItem(Items.DIAMOND), 5, "diamonds loaded");
		helper.succeed();
	}

	@GameTest
	public void shiftClickBoxOutKeepsContents(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ShulkerLoaderMenu menu = new ShulkerLoaderMenu(0, player.getInventory(), loader);
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, boxWith(0, new ItemStack(Items.DIAMOND, 4)));
		menu.getSlot(ShulkerLoaderMenu.CONTENT_START + 3).set(new ItemStack(Items.DIAMOND, 3));

		menu.clicked(ShulkerLoaderMenu.BOX_SLOT, 0, ContainerInput.QUICK_MOVE, player);
		helper.assertTrue(loader.isEmpty(), "loader emptied");
		helper.assertValueEqual(inventoryCount(player, Items.DIAMOND), 0, "no loose diamonds");
		ItemStack box = player.getInventory().getNonEquipmentItems().stream().filter(ShulkerLoaderBlockEntity::isShulkerBox).findFirst().orElse(ItemStack.EMPTY);
		helper.assertValueEqual(stored(box, Items.DIAMOND), 7, "box carries all contents");
		helper.succeed();
	}

	@GameTest
	public void contentSlotsLockedWithoutBox(GameTestHelper helper) {
		ShulkerLoaderBlockEntity loader = loader(helper);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ShulkerLoaderMenu menu = new ShulkerLoaderMenu(0, player.getInventory(), loader);
		ItemStack diamond = new ItemStack(Items.DIAMOND);
		helper.assertFalse(menu.getSlot(ShulkerLoaderMenu.CONTENT_START).mayPlace(diamond), "locked without box");
		loader.setItem(ShulkerLoaderBlockEntity.BOX_SLOT, new ItemStack(Items.SHULKER_BOX));
		helper.assertTrue(menu.getSlot(ShulkerLoaderMenu.CONTENT_START).mayPlace(diamond), "open with box");
		helper.assertFalse(menu.getSlot(ShulkerLoaderMenu.CONTENT_START).mayPlace(new ItemStack(Items.SHULKER_BOX)), "no nested boxes");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsShulkerLoaderItem(GameTestHelper helper) {
		loader(helper);
		helper.getLevel().destroyBlock(helper.absolutePos(LOADER), true);
		helper.assertItemEntityPresent(ModBlocks.SHULKER_LOADER.asItem(), LOADER, 2.0);
		helper.succeed();
	}
}
