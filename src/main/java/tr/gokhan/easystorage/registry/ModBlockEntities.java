package tr.gokhan.easystorage.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;
import tr.gokhan.easystorage.block.entity.MultiFilterHopperBlockEntity;
import tr.gokhan.easystorage.block.entity.ShulkerLoaderBlockEntity;

public final class ModBlockEntities {
	public static final BlockEntityType<FilterHopperBlockEntity> FILTER_HOPPER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("filter_hopper"),
		FabricBlockEntityTypeBuilder.create(FilterHopperBlockEntity::new, ModBlocks.FILTER_HOPPER).build()
	);

	public static final BlockEntityType<MultiFilterHopperBlockEntity> MULTI_FILTER_HOPPER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("multi_filter_hopper"),
		FabricBlockEntityTypeBuilder.create(MultiFilterHopperBlockEntity::new, ModBlocks.MULTI_FILTER_HOPPER).build()
	);

	public static final BlockEntityType<ShulkerLoaderBlockEntity> SHULKER_LOADER = Registry.register(
		BuiltInRegistries.BLOCK_ENTITY_TYPE,
		EasyStorage.id("shulker_loader"),
		FabricBlockEntityTypeBuilder.create(ShulkerLoaderBlockEntity::new, ModBlocks.SHULKER_LOADER).build()
	);

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
