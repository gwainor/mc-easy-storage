package tr.gokhan.easystorage.registry;

import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.block.ItemFeederBlock;
import tr.gokhan.easystorage.block.FilterHopperBlock;

public final class ModBlocks {
	public static final Block FILTER_HOPPER = register(
		"filter_hopper",
		FilterHopperBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()
	);

	public static final Block ITEM_FEEDER = register(
		"item_feeder",
		ItemFeederBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava()
	);

	private ModBlocks() {
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		Identifier id = EasyStorage.id(name);
		BlockItemId ids = BlockItemId.create(id, id);
		Block block = Blocks.register(ids.block(), factory, properties);
		Registry.register(BuiltInRegistries.ITEM, ids.item(), new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(ids.item())));
		return block;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
			output.accept(FILTER_HOPPER);
			output.accept(ITEM_FEEDER);
		});
	}
}
