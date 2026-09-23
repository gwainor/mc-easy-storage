package tr.gokhan.easystorage.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import tr.gokhan.easystorage.client.screen.SorterHopperScreen;
import tr.gokhan.easystorage.registry.ModMenus;

public class EasyStorageClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenus.SORTER_HOPPER, SorterHopperScreen::new);
	}
}
