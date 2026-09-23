package tr.gokhan.easystorage.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.menu.SorterHopperMenu;

public final class ModMenus {
	public static final MenuType<SorterHopperMenu> SORTER_HOPPER = Registry.register(
		BuiltInRegistries.MENU,
		EasyStorage.id("sorter_hopper"),
		new MenuType<>(SorterHopperMenu::new, FeatureFlags.VANILLA_SET)
	);

	private ModMenus() {
	}

	public static void initialize() {
	}
}
