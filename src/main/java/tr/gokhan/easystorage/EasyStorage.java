package tr.gokhan.easystorage;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tr.gokhan.easystorage.registry.ModBlockEntities;
import tr.gokhan.easystorage.registry.ModBlocks;
import tr.gokhan.easystorage.registry.ModMenus;

public class EasyStorage implements ModInitializer {
	public static final String MOD_ID = "easystorage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModMenus.initialize();
		LOGGER.info("Easy Storage initialized");
	}
}
