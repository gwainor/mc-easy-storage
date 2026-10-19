package tr.gokhan.easystorage.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.menu.FilterHopperMenu;
import tr.gokhan.easystorage.menu.ShulkerLoaderMenu;

public final class ModMenus {
	public static final MenuType<FilterHopperMenu> FILTER_HOPPER = Registry.register(
		BuiltInRegistries.MENU,
		EasyStorage.id("filter_hopper"),
		new MenuType<>(FilterHopperMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<ShulkerLoaderMenu> SHULKER_LOADER = Registry.register(
		BuiltInRegistries.MENU,
		EasyStorage.id("shulker_loader"),
		new MenuType<>(ShulkerLoaderMenu::new, FeatureFlags.VANILLA_SET)
	);

	private ModMenus() {
	}

	public static void initialize() {
	}
}
