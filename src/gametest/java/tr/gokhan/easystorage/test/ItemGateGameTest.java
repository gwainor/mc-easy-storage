package tr.gokhan.easystorage.test;

import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import tr.gokhan.easystorage.block.entity.ShulkerUnloader;

public class ItemGateGameTest {
	private static ItemStack shulkerWith(ItemStack... contents) {
		ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
		shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Arrays.asList(contents)));
		return shulker;
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
}
