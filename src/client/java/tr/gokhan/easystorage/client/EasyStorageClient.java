package tr.gokhan.easystorage.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import tr.gokhan.easystorage.client.screen.FilterHopperScreen;
import tr.gokhan.easystorage.menu.FilterHopperMenu;
import tr.gokhan.easystorage.client.screen.ShulkerLoaderScreen;
import tr.gokhan.easystorage.registry.ModMenus;

public class EasyStorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.FILTER_HOPPER, FilterHopperScreen<FilterHopperMenu>::new);
		MenuScreens.register(ModMenus.SHULKER_LOADER, ShulkerLoaderScreen::new);
	}
}
