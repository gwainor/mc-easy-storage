package tr.gokhan.easystorage.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.block.entity.ItemFeederBlockEntity;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;

public final class ModBlockEntities {
	public static final BlockEntityType<FilterHopperBlockEntity> FILTER_HOPPER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("filter_hopper"),
		FabricBlockEntityTypeBuilder.create(FilterHopperBlockEntity::new, ModBlocks.FILTER_HOPPER).build()
	);

	public static final BlockEntityType<ItemFeederBlockEntity> ITEM_FEEDER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("item_feeder"),
		FabricBlockEntityTypeBuilder.create(ItemFeederBlockEntity::new, ModBlocks.ITEM_FEEDER).build()
	);

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
