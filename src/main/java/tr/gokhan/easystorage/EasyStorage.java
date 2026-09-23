package tr.gokhan.easystorage;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyStorage implements ModInitializer {
	public static final String MOD_ID = "easystorage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Easy Storage initialized");
	}
}
