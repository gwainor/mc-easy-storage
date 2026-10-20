package tr.gokhan.easystorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import tr.gokhan.easystorage.block.entity.FilterHopperBlockEntity;
import tr.gokhan.easystorage.block.entity.MultiFilterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/** Filter Hopper variant with 27 filter slots. */
public class MultiFilterHopperBlock extends FilterHopperBlock {

	public MultiFilterHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MultiFilterHopperBlockEntity(pos, state);
	}

	@Override
	protected BlockEntityType<? extends FilterHopperBlockEntity> blockEntityType() {
		return ModBlockEntities.MULTI_FILTER_HOPPER;
	}
}
