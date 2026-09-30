package tr.gokhan.easystorage.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tr.gokhan.easystorage.block.ItemGateBlock;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * Block entity for the Item Gate: 27 slots, drops one stack every 8 ticks out of
 * its output face. Shulker boxes are unloaded stack by stack, then dropped empty.
 */
public class ItemGateBlockEntity extends RandomizableContainerBlockEntity {
	public static final int CONTAINER_SIZE = 27;
	/** Distance from the block centre to the dropped item's centre along FACING. */
	private static final double DROP_OFFSET = 0.8;
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.item_gate");

	private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
	private int cooldownTime;
	private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			ItemGateBlockEntity.this.setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			ItemGateBlockEntity.this.setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == ItemGateBlockEntity.this;
		}
	};

	public ItemGateBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ITEM_GATE, pos, state);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		if (!this.tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, this.items);
		}
		this.cooldownTime = input.getIntOr("DropCooldown", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!this.trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, this.items);
		}
		output.putInt("DropCooldown", this.cooldownTime);
	}

	@Override
	public int getContainerSize() {
		return CONTAINER_SIZE;
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	protected Component getDefaultName() {
		return DEFAULT_NAME;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	// --- Opening (drives the OPEN block state, like the vanilla barrel) ---

	@Override
	public void startOpen(ContainerUser user) {
		if (!this.remove && !user.getLivingEntity().isSpectator()) {
			this.openersCounter.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!this.remove && !user.getLivingEntity().isSpectator()) {
			this.openersCounter.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
		}
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
	}

	public void recheckOpen() {
		if (!this.remove) {
			this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
		}
	}

	private void setOpen(BlockState state, boolean open) {
		this.level.setBlock(this.getBlockPos(), state.setValue(ItemGateBlock.OPEN, open), Block.UPDATE_ALL);
	}

	// --- Dropping ---

	public static void dropTick(Level level, BlockPos pos, BlockState state, ItemGateBlockEntity gate) {
		if (gate.cooldownTime > 0) {
			gate.cooldownTime--;
			return;
		}
		if (!state.getValue(ItemGateBlock.ENABLED) || gate.isEmpty()) {
			return;
		}
		if (gate.dropNext(level, pos, state.getValue(ItemGateBlock.FACING))) {
			gate.cooldownTime = HopperBlockEntity.MOVE_ITEM_SPEED - 1;
			gate.setChanged();
		}
	}

	/** Drops one stack out of the {@code facing} face. Returns false when nothing was dropped. */
	private boolean dropNext(Level level, BlockPos pos, Direction facing) {
		BlockPos target = pos.relative(facing);
		if (level.getBlockState(target).isCollisionShapeFullBlock(level, target)) {
			return false;
		}

		ItemStack toDrop = this.takeNextStack();
		if (toDrop.isEmpty()) {
			return false;
		}

		double x = pos.getX() + 0.5 + facing.getStepX() * DROP_OFFSET;
		double y = pos.getY() + 0.5 + facing.getStepY() * DROP_OFFSET;
		double z = pos.getZ() + 0.5 + facing.getStepZ() * DROP_OFFSET;
		// Item entities are positioned by their feet; lower by half their height to centre them.
		ItemEntity entity = new ItemEntity(level, x, y - 0.125, z, toDrop, 0.0, 0.0, 0.0);
		level.addFreshEntity(entity);
		return true;
	}

	/** Removes the next stack to drop: a shulker box's next stored stack, or the whole slot. */
	private ItemStack takeNextStack() {
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			ItemStack stack = this.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (ShulkerUnloader.hasContents(stack)) {
				return ShulkerUnloader.takeNext(stack);
			}
			return this.removeItemNoUpdate(slot);
		}
		return ItemStack.EMPTY;
	}
}
