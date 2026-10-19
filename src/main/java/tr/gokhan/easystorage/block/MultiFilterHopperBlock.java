package tr.gokhan.easystorage.block;

import com.mojang.serialization.MapCodec;
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
	public static final MapCodec<MultiFilterHopperBlock> CODEC = simpleCodec(MultiFilterHopperBlock::new);

	public MultiFilterHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends FilterHopperBlock> codec() {
		return CODEC;
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
