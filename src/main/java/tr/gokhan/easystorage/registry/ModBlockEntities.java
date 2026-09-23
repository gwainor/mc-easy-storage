package tr.gokhan.easystorage.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;

public final class ModBlockEntities {
	public static final BlockEntityType<SorterHopperBlockEntity> SORTER_HOPPER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("sorter_hopper"),
		FabricBlockEntityTypeBuilder.create(SorterHopperBlockEntity::new, ModBlocks.SORTER_HOPPER).build()
	);

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
