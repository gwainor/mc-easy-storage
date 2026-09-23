package tr.gokhan.easystorage.block.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Takes stored stacks out of a shulker box item one at a time. Pure item logic,
 * no world access.
 */
public final class ShulkerUnloader {
	private ShulkerUnloader() {
	}

	/** True when {@code stack} is a shulker box that still stores at least one item. */
	public static boolean hasContents(ItemStack stack) {
		return stack.is(ItemTags.SHULKER_BOXES)
			&& stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext();
	}

	/**
	 * Removes the first non-empty stored stack from {@code shulker} and returns it.
	 * Returns {@link ItemStack#EMPTY} when nothing is stored.
	 */
	public static ItemStack takeNext(ItemStack shulker) {
		ItemContainerContents contents = shulker.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		List<ItemStack> items = new ArrayList<>(contents.allItemsCopyStream().toList());
		for (int i = 0; i < items.size(); i++) {
			if (!items.get(i).isEmpty()) {
				ItemStack taken = items.set(i, ItemStack.EMPTY);
				shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
				return taken;
			}
		}
		return ItemStack.EMPTY;
	}
}
