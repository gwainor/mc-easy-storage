package tr.gokhan.easystorage.test;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import tr.gokhan.easystorage.block.entity.ShulkerLoaderBlockEntity;
import tr.gokhan.easystorage.registry.ModBlocks;

public class ShulkerLoaderGameTest {
	private static final BlockPos LOADER = new BlockPos(1, 2, 1);
	private static final BlockPos ABOVE = new BlockPos(1, 3, 1);

	private static ShulkerLoaderBlockEntity loader(GameTestHelper helper) {
		helper.setBlock(LOADER, ModBlocks.SHULKER_LOADER);
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
}
