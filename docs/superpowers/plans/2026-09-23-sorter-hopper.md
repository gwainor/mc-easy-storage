# Sorter Hopper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Sorter Hopper block: a hopper that only accepts items matching a real filter item, holds 5 stacks, and needs no redstone.

**Architecture:** Own `SorterHopperBlock` (copy of vanilla hopper block behaviour) plus `SorterHopperBlockEntity` extending `BaseContainerBlockEntity` and implementing vanilla `Hopper`. The block entity's `Container` is the 5-slot buffer only; the filter is a separate 1-slot container. Filtering is a single `canPlaceItem` override, which vanilla's public `HopperBlockEntity.suckInItems/addItem` helpers already honour. Tick logic mirrors the vanilla hopper (8-tick cooldown). Verified with Fabric GameTests.

**Tech Stack:** Java 25, Minecraft 26.2 (Mojang names, unobfuscated), Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Loom 1.17-SNAPSHOT, Fabric GameTest API.

**Spec:** `docs/superpowers/specs/2026-09-23-sorter-hopper-design.md`

## Global Constraints

- Minecraft `~26.2`, Java `>=25`. Every Gradle command runs with `export JAVA_HOME=/opt/homebrew/opt/openjdk@25` (Java 25 is keg-only; the system `java` is 8).
- Mod id `easystorage`; Java package `tr.gokhan.easystorage`; registry ids use `EasyStorage.id(path)`.
- No mixins, no access wideners.
- Filter match = item type only: `stack.is(filter.getItem())`.
- Buffer = 5 slots; filter = 1 real item, max stack 1, never reachable by automation.
- Transfer speed = vanilla hopper (`HopperBlockEntity.MOVE_ITEM_SPEED` = 8 ticks).
- Recipe: shapeless hopper + comparator + observer → 1 `easystorage:sorter_hopper`.
- Do not copy Mojang texture PNGs into the repo. Placeholder art only (generated).
- Code style: tabs, matches existing `EasyStorage.java`.

## Reference notes (26.2 API facts, verified)

- `Blocks.register(ResourceKey<Block>, Function<Properties, Block>, Properties)` and `new MenuType<>(supplier, FeatureFlags.VANILLA_SET)` and `MenuScreens.register(...)` are public via Fabric's transitive access wideners.
- `BlockItemId.create(Identifier, Identifier)` gives block and item `ResourceKey`s.
- Block entity persistence uses `ValueInput` / `ValueOutput` (`input.read(name, codec)`, `output.store(name, codec, value)`).
- Container contents drop in `BlockEntity.preRemoveSideEffects`, which is called for any removal of the block.
- Screens override `extractBackground(GuiGraphicsExtractor, int, int, float)` (renamed from `renderBg`).
- GameTests: `@net.fabricmc.fabric.api.gametest.v1.GameTest` on instance methods taking `GameTestHelper`; default structure is empty; they run in `./gradlew runGameTest` and as part of `./gradlew build`. `helper.destroyBlock` does NOT drop loot; use `helper.getLevel().destroyBlock(helper.absolutePos(pos), true)`.
- Fabric's harness adds one built-in test, so "All N required tests passed" shows our count + 1.

## File Structure

| File | Responsibility |
|---|---|
| `build.gradle` (modify) | Enable GameTest source set. |
| `src/gametest/resources/fabric.mod.json` | Test mod metadata + `fabric-gametest` entrypoint. |
| `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java` | All in-world tests. |
| `src/main/java/tr/gokhan/easystorage/EasyStorage.java` (modify) | `id()` helper; call registry initializers. |
| `src/main/java/tr/gokhan/easystorage/registry/ModBlocks.java` | Block + block item registration; creative tab entry. |
| `src/main/java/tr/gokhan/easystorage/registry/ModBlockEntities.java` | Block entity type registration. |
| `src/main/java/tr/gokhan/easystorage/registry/ModMenus.java` | Menu type registration. |
| `src/main/java/tr/gokhan/easystorage/block/SorterHopperBlock.java` | Block: facing, enabled, shape, placement, redstone, comparator, menu opening. |
| `src/main/java/tr/gokhan/easystorage/block/entity/SorterHopperBlockEntity.java` | Buffer + filter storage, filter rule, NBT, drops, tick transfer logic. |
| `src/main/java/tr/gokhan/easystorage/menu/SorterHopperMenu.java` | Slots + shift-click rules. |
| `src/client/java/tr/gokhan/easystorage/client/EasyStorageClient.java` (modify) | Register screen. |
| `src/client/java/tr/gokhan/easystorage/client/screen/SorterHopperScreen.java` | Placeholder screen drawn with fills. |
| `src/main/resources/assets/easystorage/...` | Blockstate, models (parented to vanilla hopper models), item definition, lang, textures. |
| `src/main/resources/data/easystorage/...`, `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json` | Loot table, recipe, recipe advancement, pickaxe tag. |
| `tools/placeholder_textures.py` | Deterministic stdlib-only PNG generator for placeholder textures. |

(The spec's `ModItems` is folded into `ModBlocks`: the only item is the block item.)

---

### Task 1: GameTest harness and Sorter Hopper core (registration, storage, filter, menu)

**Files:**
- Modify: `build.gradle`
- Create: `src/gametest/resources/fabric.mod.json`
- Create: `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java`
- Modify: `src/main/java/tr/gokhan/easystorage/EasyStorage.java`
- Create: `src/main/java/tr/gokhan/easystorage/registry/ModBlocks.java`, `src/main/java/tr/gokhan/easystorage/registry/ModBlockEntities.java`, `src/main/java/tr/gokhan/easystorage/registry/ModMenus.java`
- Create: `src/main/java/tr/gokhan/easystorage/block/SorterHopperBlock.java` (no ticking yet)
- Create: `src/main/java/tr/gokhan/easystorage/block/entity/SorterHopperBlockEntity.java` (no ticking yet)
- Create: `src/main/java/tr/gokhan/easystorage/menu/SorterHopperMenu.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `EasyStorage.id(String path) -> Identifier`
  - `ModBlocks.SORTER_HOPPER : Block`, `ModBlockEntities.SORTER_HOPPER : BlockEntityType<SorterHopperBlockEntity>`, `ModMenus.SORTER_HOPPER : MenuType<SorterHopperMenu>`
  - `SorterHopperBlock.FACING : EnumProperty<Direction>`, `SorterHopperBlock.ENABLED : BooleanProperty`
  - `SorterHopperBlockEntity.BUFFER_SIZE = 5`; `getFilter() -> Container`; `getFilterItem() -> ItemStack`; `setFilterItem(ItemStack)` (stores a copy with count 1); `static matchesFilter(ItemStack filterItem, ItemStack stack) -> boolean`; `canPlaceItem(int, ItemStack)` enforces the filter.
  - `SorterHopperMenu(int, Inventory)` (client) and `SorterHopperMenu(int, Inventory, Container buffer, Container filter)`; constants `FILTER_SLOT=0`, `BUFFER_START=1`, `BUFFER_END=6`, `FILTER_X=17`, `BUFFER_X=62`, `SLOT_Y=20`.

- [ ] **Step 1: Enable the GameTest source set**

In `build.gradle`, insert this block directly above the `dependencies {` block:

```gradle
fabricApi {
	configureTests {
		createSourceSet = true
		modId = "easystorage-test"
		enableGameTests = true
		enableClientGameTests = false
		eula = true
	}
}
```

Create `src/gametest/resources/fabric.mod.json`:

```json
{
	"schemaVersion": 1,
	"id": "easystorage-test",
	"version": "1.0.0",
	"name": "Easy Storage Tests",
	"environment": "*",
	"entrypoints": {
		"fabric-gametest": [
			"tr.gokhan.easystorage.test.SorterHopperGameTest"
		]
	},
	"depends": {
		"easystorage": "*"
	}
}
```

- [ ] **Step 2: Write the failing tests**

Create `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java`:

```java
package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

public class SorterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos SORTER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	@GameTest
	public void comparatorIgnoresFilterSlot(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.DIAMOND));
		helper.assertValueEqual(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter), 0, "signal with filter only");

		sorter.setItem(0, new ItemStack(Items.DIAMOND));
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter) > 0, "signal with buffered item");
		helper.succeed();
	}

	@GameTest
	public void breakingDropsBufferAndFilter(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.EMERALD));
		sorter.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.getLevel().destroyBlock(helper.absolutePos(SORTER), true);

		helper.assertItemEntityPresent(Items.DIAMOND, SORTER, 2.0);
		helper.assertItemEntityPresent(Items.EMERALD, SORTER, 2.0);
		helper.succeed();
	}

	@GameTest
	public void shiftClickFillsFilterThenBuffer(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		SorterHopperMenu menu = new SorterHopperMenu(0, player.getInventory(), sorter, sorter.getFilter());
		// Menu slots: 0 = filter, 1-5 = buffer, 6-32 = main inventory, 33-41 = hotbar.
		int diamondSlot = SorterHopperMenu.BUFFER_END;
		int dirtSlot = SorterHopperMenu.BUFFER_END + 1;
		menu.getSlot(diamondSlot).set(new ItemStack(Items.DIAMOND, 5));
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 3));

		menu.quickMoveStack(player, diamondSlot);
		helper.assertTrue(sorter.getFilterItem().is(Items.DIAMOND), "first shift-click sets filter");
		helper.assertValueEqual(sorter.getFilterItem().getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(diamondSlot).getItem().getCount(), 4, "diamonds left in inventory");

		menu.quickMoveStack(player, diamondSlot);
		helper.assertValueEqual(sorter.countItem(Items.DIAMOND), 4, "diamonds in buffer");

		menu.quickMoveStack(player, dirtSlot);
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 3, "dirt stays in inventory");
		helper.assertValueEqual(sorter.countItem(Items.DIRT), 0, "dirt in buffer");
		helper.succeed();
	}
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runGameTest`
Expected: FAIL at `:compileGametestJava` with `package tr.gokhan.easystorage.block.entity does not exist` (and similar for `menu`, `registry`).

- [ ] **Step 4: Add the `id` helper and registry calls**

Replace `src/main/java/tr/gokhan/easystorage/EasyStorage.java`:

```java
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
```

- [ ] **Step 5: Create the registries**

`src/main/java/tr/gokhan/easystorage/registry/ModBlocks.java`:

```java
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
import net.minecraft.world.level.material.MapColor;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.block.SorterHopperBlock;

public final class ModBlocks {
	public static final Block SORTER_HOPPER = register(
		"sorter_hopper",
		SorterHopperBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 4.8F).sound(SoundType.METAL).noOcclusion()
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
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> output.accept(SORTER_HOPPER));
	}
}
```

`src/main/java/tr/gokhan/easystorage/registry/ModBlockEntities.java`:

```java
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
```

`src/main/java/tr/gokhan/easystorage/registry/ModMenus.java`:

```java
package tr.gokhan.easystorage.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import tr.gokhan.easystorage.EasyStorage;
import tr.gokhan.easystorage.menu.SorterHopperMenu;

public final class ModMenus {
	public static final MenuType<SorterHopperMenu> SORTER_HOPPER = Registry.register(
		BuiltInRegistries.MENU,
		EasyStorage.id("sorter_hopper"),
		new MenuType<>(SorterHopperMenu::new, FeatureFlags.VANILLA_SET)
	);

	private ModMenus() {
	}

	public static void initialize() {
	}
}
```

- [ ] **Step 6: Create the block (no ticking yet)**

`src/main/java/tr/gokhan/easystorage/block/SorterHopperBlock.java`:

```java
package tr.gokhan.easystorage.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;

/**
 * A hopper that only accepts items matching its filter item. Shape, placement,
 * redstone locking and comparator output mirror the vanilla {@link net.minecraft.world.level.block.HopperBlock}.
 */
public class SorterHopperBlock extends BaseEntityBlock {
	public static final MapCodec<SorterHopperBlock> CODEC = simpleCodec(SorterHopperBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private final Function<BlockState, VoxelShape> shapes;
	private final Map<Direction, VoxelShape> interactionShapes;

	public SorterHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
		VoxelShape inside = Block.column(12.0, 11.0, 16.0);
		this.shapes = this.makeShapes(inside);
		this.interactionShapes = ImmutableMap.<Direction, VoxelShape>builderWithExpectedSize(5)
			.putAll(Shapes.rotateHorizontal(Shapes.or(inside, Block.boxZ(4.0, 8.0, 10.0, 0.0, 4.0))))
			.put(Direction.DOWN, inside)
			.build();
	}

	@Override
	public MapCodec<SorterHopperBlock> codec() {
		return CODEC;
	}

	private Function<BlockState, VoxelShape> makeShapes(VoxelShape inside) {
		VoxelShape outline = Shapes.or(Block.column(16.0, 10.0, 16.0), Block.column(8.0, 4.0, 10.0));
		VoxelShape bowl = Shapes.join(outline, inside, BooleanOp.ONLY_FIRST);
		Map<Direction, VoxelShape> spouts = Shapes.rotateAll(Block.boxZ(4.0, 4.0, 8.0, 0.0, 8.0), new Vec3(8.0, 6.0, 8.0).scale(0.0625));
		return this.getShapeForEachState(
			state -> Shapes.or(bowl, Shapes.join(spouts.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND)), ENABLED
		);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shapes.apply(state);
	}

	@Override
	protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return this.interactionShapes.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getClickedFace().getOpposite();
		return this.defaultBlockState()
			.setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction)
			.setValue(ENABLED, true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SorterHopperBlockEntity(pos, state);
	}


	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(state.getBlock())) {
			this.checkPoweredState(level, pos, state);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SorterHopperBlockEntity sorter) {
			player.openMenu(sorter);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		this.checkPoweredState(level, pos, state);
	}

	private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
		boolean shouldBeOn = !level.hasNeighborSignal(pos);
		if (shouldBeOn != state.getValue(ENABLED)) {
			level.setBlock(pos, state.setValue(ENABLED, shouldBeOn), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		// The block entity's Container is the buffer only, so the filter never counts.
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ENABLED);
	}


	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
```

- [ ] **Step 7: Create the block entity (no ticking yet)**

`src/main/java/tr/gokhan/easystorage/block/entity/SorterHopperBlockEntity.java`:

```java
package tr.gokhan.easystorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * Block entity for the Sorter Hopper. The {@link Container} it exposes is the
 * 5-slot buffer only; the filter lives in a separate 1-slot container that
 * automation never sees.
 */
public class SorterHopperBlockEntity extends BaseContainerBlockEntity implements Hopper {
	public static final int BUFFER_SIZE = 5;
	private static final int NO_COOLDOWN = -1;
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.sorter_hopper");

	private NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
	private final SimpleContainer filter = new SimpleContainer(1) {
		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			SorterHopperBlockEntity.this.setChanged();
		}
	};
	private int cooldownTime = NO_COOLDOWN;

	public SorterHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SORTER_HOPPER, pos, state);
	}

	public Container getFilter() {
		return this.filter;
	}

	public ItemStack getFilterItem() {
		return this.filter.getItem(0);
	}

	public void setFilterItem(ItemStack stack) {
		this.filter.setItem(0, stack.copyWithCount(1));
	}

	/** True when {@code stack} has the same item type as the filter. Components are ignored. */
	public static boolean matchesFilter(ItemStack filterItem, ItemStack stack) {
		return !filterItem.isEmpty() && stack.is(filterItem.getItem());
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return matchesFilter(this.getFilterItem(), stack);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.filter.setItem(0, input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		this.cooldownTime = input.getIntOr("TransferCooldown", NO_COOLDOWN);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		ItemStack filterItem = this.getFilterItem();
		if (!filterItem.isEmpty()) {
			output.store("filter", ItemStack.CODEC, filterItem);
		}
		output.putInt("TransferCooldown", this.cooldownTime);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level != null) {
			Containers.dropContents(this.level, pos, this.filter);
		}
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
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
		return new SorterHopperMenu(containerId, inventory, this, this.filter);
	}

	// --- Hopper ---

	@Override
	public double getLevelX() {
		return this.worldPosition.getX() + 0.5;
	}

	@Override
	public double getLevelY() {
		return this.worldPosition.getY() + 0.5;
	}

	@Override
	public double getLevelZ() {
		return this.worldPosition.getZ() + 0.5;
	}

	@Override
	public boolean isGridAligned() {
		return true;
	}
}
```

- [ ] **Step 8: Create the menu**

`src/main/java/tr/gokhan/easystorage/menu/SorterHopperMenu.java`:

```java
package tr.gokhan.easystorage.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModMenus;

public class SorterHopperMenu extends AbstractContainerMenu {
	public static final int FILTER_SLOT = 0;
	public static final int BUFFER_START = 1;
	public static final int BUFFER_END = BUFFER_START + SorterHopperBlockEntity.BUFFER_SIZE;
	public static final int FILTER_X = 17;
	public static final int BUFFER_X = 62;
	public static final int SLOT_Y = 20;

	private final Container buffer;
	private final Container filter;

	/** Client-side constructor. */
	public SorterHopperMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(SorterHopperBlockEntity.BUFFER_SIZE), new SimpleContainer(1));
	}

	public SorterHopperMenu(int containerId, Inventory inventory, Container buffer, Container filter) {
		super(ModMenus.SORTER_HOPPER, containerId);
		checkContainerSize(buffer, SorterHopperBlockEntity.BUFFER_SIZE);
		checkContainerSize(filter, 1);
		this.buffer = buffer;
		this.filter = filter;
		buffer.startOpen(inventory.player);

		this.addSlot(new FilterSlot(filter, 0, FILTER_X, SLOT_Y));
		for (int i = 0; i < SorterHopperBlockEntity.BUFFER_SIZE; i++) {
			this.addSlot(new BufferSlot(buffer, i, BUFFER_X + i * 18, SLOT_Y));
		}
		this.addStandardInventorySlots(inventory, 8, 51);
	}

	private ItemStack filterItem() {
		return this.filter.getItem(0);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.buffer.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < BUFFER_END) {
			if (!this.moveItemStackTo(stack, BUFFER_END, this.slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (this.filterItem().isEmpty()) {
			if (!this.moveItemStackTo(stack, FILTER_SLOT, FILTER_SLOT + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (SorterHopperBlockEntity.matchesFilter(this.filterItem(), stack)) {
			if (!this.moveItemStackTo(stack, BUFFER_START, BUFFER_END, false)) {
				return ItemStack.EMPTY;
			}
		} else {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.buffer.stopOpen(player);
	}

	private static class FilterSlot extends Slot {
		FilterSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	private class BufferSlot extends Slot {
		BufferSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return SorterHopperBlockEntity.matchesFilter(SorterHopperMenu.this.filterItem(), stack);
		}
	}
}
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew build`
Expected: `BUILD SUCCESSFUL` and log line `All 4 required tests passed :)` (3 ours + 1 Fabric built-in).

- [ ] **Step 10: Commit**

```bash
git add build.gradle src/gametest src/main/java
git commit -m "feat: add Sorter Hopper block, block entity and menu"
```

---

### Task 2: Transfer logic (pull, filter, push, redstone lock)

**Files:**
- Modify: `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java` (replace whole file)
- Modify: `src/main/java/tr/gokhan/easystorage/block/entity/SorterHopperBlockEntity.java` (replace whole file)
- Modify: `src/main/java/tr/gokhan/easystorage/block/SorterHopperBlock.java` (replace whole file)

**Interfaces:**
- Consumes: everything Task 1 produces.
- Produces:
  - `SorterHopperBlockEntity.pushItemsTick(Level, BlockPos, BlockState, SorterHopperBlockEntity)` (static ticker)
  - `SorterHopperBlockEntity.entityInside(Level, BlockPos, BlockState, Entity, SorterHopperBlockEntity)` (static)
  - `SorterHopperBlock.getTicker(...)` and `entityInside(...)` wired to the above.

- [ ] **Step 1: Write the failing transfer tests**

Replace `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java` with (adds 6 transfer tests and a `column` helper to the Task 1 tests):

```java
package tr.gokhan.easystorage.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import tr.gokhan.easystorage.block.SorterHopperBlock;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlocks;

public class SorterHopperGameTest {
	private static final BlockPos TOP = new BlockPos(1, 3, 1);
	private static final BlockPos SORTER = new BlockPos(1, 2, 1);
	private static final BlockPos BOTTOM = new BlockPos(1, 1, 1);

	/** Chest above, sorter (facing down) in the middle, chest below. */
	private static SorterHopperBlockEntity column(GameTestHelper helper, ItemStack filter) {
		helper.setBlock(TOP, Blocks.CHEST);
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(filter);
		return sorter;
	}

	private static Container container(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockEntity(pos, BaseContainerBlockEntity.class);
	}

	@GameTest(maxTicks = 40)
	public void matchingItemIsPulledAndPushed(GameTestHelper helper) {
		column(helper, new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.succeedWhen(() -> helper.assertContainerContains(BOTTOM, Items.DIAMOND));
	}

	@GameTest(maxTicks = 60)
	public void nonMatchingItemContinuesAlongLine(GameTestHelper helper) {
		// source chest -> line hopper (east) -> next hopper (down) -> overflow chest
		//                 sorter below line hopper -> sorted chest
		BlockPos source = new BlockPos(1, 4, 1);
		BlockPos lineHopper = new BlockPos(1, 3, 1);
		BlockPos nextHopper = new BlockPos(2, 3, 1);
		BlockPos overflow = new BlockPos(2, 2, 1);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(lineHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
		helper.setBlock(nextHopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(overflow, Blocks.CHEST);
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.setBlock(BOTTOM, Blocks.CHEST);
		helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class).setFilterItem(new ItemStack(Items.DIAMOND));

		container(helper, source).setItem(0, new ItemStack(Items.DIAMOND));
		container(helper, source).setItem(1, new ItemStack(Items.DIRT));

		helper.succeedWhen(() -> {
			helper.assertContainerContains(BOTTOM, Items.DIAMOND);
			helper.assertContainerContains(overflow, Items.DIRT);
			helper.assertFalse(container(helper, BOTTOM).countItem(Items.DIRT) > 0, "dirt must not be sorted");
			helper.assertFalse(container(helper, overflow).countItem(Items.DIAMOND) > 0, "diamond must be sorted");
		});
	}

	@GameTest(maxTicks = 30)
	public void emptyFilterAcceptsNothing(GameTestHelper helper) {
		column(helper, ItemStack.EMPTY);
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(SORTER);
			helper.assertContainerEmpty(BOTTOM);
			helper.assertContainerContains(TOP, Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 30)
	public void redstoneSignalLocksTransfers(GameTestHelper helper) {
		column(helper, new ItemStack(Items.DIAMOND));
		helper.setBlock(new BlockPos(2, 2, 1), Blocks.REDSTONE_BLOCK);
		container(helper, TOP).setItem(0, new ItemStack(Items.DIAMOND));
		helper.runAfterDelay(20, () -> {
			helper.assertBlockProperty(SORTER, SorterHopperBlock.ENABLED, false);
			helper.assertContainerEmpty(SORTER);
			helper.assertContainerEmpty(BOTTOM);
			helper.assertContainerContains(TOP, Items.DIAMOND);
			helper.succeed();
		});
	}

	@GameTest
	public void comparatorIgnoresFilterSlot(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.DIAMOND));
		helper.assertValueEqual(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter), 0, "signal with filter only");

		sorter.setItem(0, new ItemStack(Items.DIAMOND));
		helper.assertTrue(AbstractContainerMenu.getRedstoneSignalFromBlockEntity(sorter) > 0, "signal with buffered item");
		helper.succeed();
	}

	@GameTest(maxTicks = 40)
	public void unstackableFilterIgnoresComponents(GameTestHelper helper) {
		column(helper, new ItemStack(Items.IRON_SWORD));
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		sword.setDamageValue(10);
		sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 2);
		container(helper, TOP).setItem(0, sword);
		helper.succeedWhen(() -> helper.assertContainerContains(BOTTOM, Items.IRON_SWORD));
	}

	@GameTest
	public void breakingDropsBufferAndFilter(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.EMERALD));
		sorter.setItem(0, new ItemStack(Items.DIAMOND, 3));

		helper.getLevel().destroyBlock(helper.absolutePos(SORTER), true);

		helper.assertItemEntityPresent(Items.DIAMOND, SORTER, 2.0);
		helper.assertItemEntityPresent(Items.EMERALD, SORTER, 2.0);
		helper.succeed();
	}

	@GameTest(maxTicks = 30)
	public void hopperCannotFillFilterOrPushNonMatching(GameTestHelper helper) {
		helper.setBlock(TOP, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		sorter.setFilterItem(new ItemStack(Items.DIAMOND));
		container(helper, TOP).setItem(0, new ItemStack(Items.DIRT));

		helper.runAfterDelay(20, () -> {
			helper.assertContainerEmpty(SORTER);
			helper.assertTrue(sorter.getFilterItem().is(Items.DIAMOND), "filter must be unchanged");
			helper.assertContainerContains(TOP, Items.DIRT);
			helper.succeed();
		});
	}

	@GameTest
	public void shiftClickFillsFilterThenBuffer(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		SorterHopperBlockEntity sorter = helper.getBlockEntity(SORTER, SorterHopperBlockEntity.class);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		SorterHopperMenu menu = new SorterHopperMenu(0, player.getInventory(), sorter, sorter.getFilter());
		// Menu slots: 0 = filter, 1-5 = buffer, 6-32 = main inventory, 33-41 = hotbar.
		int diamondSlot = SorterHopperMenu.BUFFER_END;
		int dirtSlot = SorterHopperMenu.BUFFER_END + 1;
		menu.getSlot(diamondSlot).set(new ItemStack(Items.DIAMOND, 5));
		menu.getSlot(dirtSlot).set(new ItemStack(Items.DIRT, 3));

		menu.quickMoveStack(player, diamondSlot);
		helper.assertTrue(sorter.getFilterItem().is(Items.DIAMOND), "first shift-click sets filter");
		helper.assertValueEqual(sorter.getFilterItem().getCount(), 1, "filter count");
		helper.assertValueEqual(menu.getSlot(diamondSlot).getItem().getCount(), 4, "diamonds left in inventory");

		menu.quickMoveStack(player, diamondSlot);
		helper.assertValueEqual(sorter.countItem(Items.DIAMOND), 4, "diamonds in buffer");

		menu.quickMoveStack(player, dirtSlot);
		helper.assertValueEqual(menu.getSlot(dirtSlot).getItem().getCount(), 3, "dirt stays in inventory");
		helper.assertValueEqual(sorter.countItem(Items.DIRT), 0, "dirt in buffer");
		helper.succeed();
	}

}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runGameTest`
Expected: FAIL. At least `matchingItemIsPulledAndPushed`, `nonMatchingItemContinuesAlongLine` and `unstackableFilterIgnoresComponents` time out, because nothing ticks yet. (`emptyFilterAcceptsNothing`, `redstoneSignalLocksTransfers` and `hopperCannotFillFilterOrPushNonMatching` may already pass; they check that nothing moves.)

- [ ] **Step 3: Add tick logic to the block entity**

Replace `src/main/java/tr/gokhan/easystorage/block/entity/SorterHopperBlockEntity.java`:

```java
package tr.gokhan.easystorage.block.entity;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import tr.gokhan.easystorage.block.SorterHopperBlock;
import tr.gokhan.easystorage.menu.SorterHopperMenu;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * Block entity for the Sorter Hopper. The {@link Container} it exposes is the
 * 5-slot buffer only; the filter lives in a separate 1-slot container that
 * automation never sees. Transfer timing follows the vanilla hopper.
 */
public class SorterHopperBlockEntity extends BaseContainerBlockEntity implements Hopper {
	public static final int BUFFER_SIZE = 5;
	private static final int NO_COOLDOWN = -1;
	private static final Component DEFAULT_NAME = Component.translatable("container.easystorage.sorter_hopper");

	private NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SIZE, ItemStack.EMPTY);
	private final SimpleContainer filter = new SimpleContainer(1) {
		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			SorterHopperBlockEntity.this.setChanged();
		}
	};
	private int cooldownTime = NO_COOLDOWN;

	public SorterHopperBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SORTER_HOPPER, pos, state);
	}

	public Container getFilter() {
		return this.filter;
	}

	public ItemStack getFilterItem() {
		return this.filter.getItem(0);
	}

	public void setFilterItem(ItemStack stack) {
		this.filter.setItem(0, stack.copyWithCount(1));
	}

	/** True when {@code stack} has the same item type as the filter. Components are ignored. */
	public static boolean matchesFilter(ItemStack filterItem, ItemStack stack) {
		return !filterItem.isEmpty() && stack.is(filterItem.getItem());
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return matchesFilter(this.getFilterItem(), stack);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, this.items);
		this.filter.setItem(0, input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY));
		this.cooldownTime = input.getIntOr("TransferCooldown", NO_COOLDOWN);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, this.items);
		ItemStack filterItem = this.getFilterItem();
		if (!filterItem.isEmpty()) {
			output.store("filter", ItemStack.CODEC, filterItem);
		}
		output.putInt("TransferCooldown", this.cooldownTime);
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (this.level != null) {
			Containers.dropContents(this.level, pos, this.filter);
		}
	}

	@Override
	public int getContainerSize() {
		return this.items.size();
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
		return new SorterHopperMenu(containerId, inventory, this, this.filter);
	}

	// --- Hopper ---

	@Override
	public double getLevelX() {
		return this.worldPosition.getX() + 0.5;
	}

	@Override
	public double getLevelY() {
		return this.worldPosition.getY() + 0.5;
	}

	@Override
	public double getLevelZ() {
		return this.worldPosition.getZ() + 0.5;
	}

	@Override
	public boolean isGridAligned() {
		return true;
	}

	// --- Ticking (mirrors HopperBlockEntity) ---

	public static void pushItemsTick(Level level, BlockPos pos, BlockState state, SorterHopperBlockEntity sorter) {
		sorter.cooldownTime--;
		if (!sorter.isOnCooldown()) {
			sorter.cooldownTime = 0;
			sorter.tryMoveItems(level, pos, state, () -> HopperBlockEntity.suckInItems(level, sorter));
		}
	}

	public static void entityInside(Level level, BlockPos pos, BlockState state, Entity entity, SorterHopperBlockEntity sorter) {
		if (entity instanceof ItemEntity itemEntity
			&& !itemEntity.getItem().isEmpty()
			&& entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(sorter.getSuckAabb())) {
			sorter.tryMoveItems(level, pos, state, () -> HopperBlockEntity.addItem(sorter, itemEntity));
		}
	}

	private boolean tryMoveItems(Level level, BlockPos pos, BlockState state, BooleanSupplier pull) {
		if (level.isClientSide() || this.isOnCooldown() || !state.getValue(SorterHopperBlock.ENABLED)) {
			return false;
		}

		boolean changed = false;
		if (!this.isEmpty()) {
			changed = this.ejectItems(level, pos, state);
		}
		if (!this.isFull()) {
			changed |= pull.getAsBoolean();
		}

		if (changed) {
			this.cooldownTime = HopperBlockEntity.MOVE_ITEM_SPEED;
			setChanged(level, pos, state);
			return true;
		}
		return false;
	}

	private boolean ejectItems(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(SorterHopperBlock.FACING);
		Container target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
		if (target == null) {
			return false;
		}

		Direction face = facing.getOpposite();
		for (int slot = 0; slot < this.getContainerSize(); slot++) {
			ItemStack stack = this.getItem(slot);
			if (!stack.isEmpty()) {
				int originalCount = stack.getCount();
				ItemStack rest = HopperBlockEntity.addItem(this, target, this.removeItem(slot, 1), face);
				if (rest.isEmpty()) {
					target.setChanged();
					return true;
				}
				stack.setCount(originalCount);
				if (originalCount == 1) {
					this.setItem(slot, stack);
				}
			}
		}
		return false;
	}

	private boolean isFull() {
		for (ItemStack stack : this.items) {
			if (stack.isEmpty() || stack.getCount() != stack.getMaxStackSize()) {
				return false;
			}
		}
		return true;
	}

	private boolean isOnCooldown() {
		return this.cooldownTime > 0;
	}
}
```

- [ ] **Step 4: Wire the ticker and item-entity pickup into the block**

Replace `src/main/java/tr/gokhan/easystorage/block/SorterHopperBlock.java`:

```java
package tr.gokhan.easystorage.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import tr.gokhan.easystorage.block.entity.SorterHopperBlockEntity;
import tr.gokhan.easystorage.registry.ModBlockEntities;

/**
 * A hopper that only accepts items matching its filter item. Shape, placement,
 * redstone locking and comparator output mirror the vanilla {@link net.minecraft.world.level.block.HopperBlock}.
 */
public class SorterHopperBlock extends BaseEntityBlock {
	public static final MapCodec<SorterHopperBlock> CODEC = simpleCodec(SorterHopperBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
	public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
	private final Function<BlockState, VoxelShape> shapes;
	private final Map<Direction, VoxelShape> interactionShapes;

	public SorterHopperBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
		VoxelShape inside = Block.column(12.0, 11.0, 16.0);
		this.shapes = this.makeShapes(inside);
		this.interactionShapes = ImmutableMap.<Direction, VoxelShape>builderWithExpectedSize(5)
			.putAll(Shapes.rotateHorizontal(Shapes.or(inside, Block.boxZ(4.0, 8.0, 10.0, 0.0, 4.0))))
			.put(Direction.DOWN, inside)
			.build();
	}

	@Override
	public MapCodec<SorterHopperBlock> codec() {
		return CODEC;
	}

	private Function<BlockState, VoxelShape> makeShapes(VoxelShape inside) {
		VoxelShape outline = Shapes.or(Block.column(16.0, 10.0, 16.0), Block.column(8.0, 4.0, 10.0));
		VoxelShape bowl = Shapes.join(outline, inside, BooleanOp.ONLY_FIRST);
		Map<Direction, VoxelShape> spouts = Shapes.rotateAll(Block.boxZ(4.0, 4.0, 8.0, 0.0, 8.0), new Vec3(8.0, 6.0, 8.0).scale(0.0625));
		return this.getShapeForEachState(
			state -> Shapes.or(bowl, Shapes.join(spouts.get(state.getValue(FACING)), Shapes.block(), BooleanOp.AND)), ENABLED
		);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return this.shapes.apply(state);
	}

	@Override
	protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return this.interactionShapes.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction direction = context.getClickedFace().getOpposite();
		return this.defaultBlockState()
			.setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction)
			.setValue(ENABLED, true);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SorterHopperBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SORTER_HOPPER, SorterHopperBlockEntity::pushItemsTick);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(state.getBlock())) {
			this.checkPoweredState(level, pos, state);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SorterHopperBlockEntity sorter) {
			player.openMenu(sorter);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		this.checkPoweredState(level, pos, state);
	}

	private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
		boolean shouldBeOn = !level.hasNeighborSignal(pos);
		if (shouldBeOn != state.getValue(ENABLED)) {
			level.setBlock(pos, state.setValue(ENABLED, shouldBeOn), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		Containers.updateNeighboursAfterDestroy(state, level, pos);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		// The block entity's Container is the buffer only, so the filter never counts.
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ENABLED);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (level.getBlockEntity(pos) instanceof SorterHopperBlockEntity sorter) {
			SorterHopperBlockEntity.entityInside(level, pos, state, entity, sorter);
		}
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew build`
Expected: `BUILD SUCCESSFUL`, `All 10 required tests passed :)`.

- [ ] **Step 6: Prove the filter tests have teeth**

Temporarily change `canPlaceItem` in `SorterHopperBlockEntity` to `return true;` and run `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runGameTest`.
Expected: FAIL with 3 failures: `hopperCannotFillFilterOrPushNonMatching`, `emptyFilterAcceptsNothing`, `nonMatchingItemContinuesAlongLine`. Revert the change (`git diff` must show no change to `canPlaceItem`), re-run, expect pass.

- [ ] **Step 7: Commit**

```bash
git add src
git commit -m "feat: sorter hopper transfers matching items like a vanilla hopper"
```

---

### Task 3: Screen, assets, recipe and loot

**Files:**
- Modify: `src/client/java/tr/gokhan/easystorage/client/EasyStorageClient.java`
- Create: `src/client/java/tr/gokhan/easystorage/client/screen/SorterHopperScreen.java`
- Create: `tools/placeholder_textures.py` (+ generated `src/main/resources/assets/easystorage/textures/block/sorter_hopper_{outside,top,inside}.png`)
- Create: `src/main/resources/assets/easystorage/blockstates/sorter_hopper.json`
- Create: `src/main/resources/assets/easystorage/models/block/sorter_hopper.json`, `sorter_hopper_side.json`
- Create: `src/main/resources/assets/easystorage/items/sorter_hopper.json`
- Create: `src/main/resources/assets/easystorage/lang/en_us.json`
- Create: `src/main/resources/data/easystorage/loot_table/blocks/sorter_hopper.json`
- Create: `src/main/resources/data/easystorage/recipe/sorter_hopper.json`
- Create: `src/main/resources/data/easystorage/advancement/recipes/redstone/sorter_hopper.json`
- Create: `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`
- Modify: `src/gametest/java/tr/gokhan/easystorage/test/SorterHopperGameTest.java` (append one test)

**Interfaces:**
- Consumes: `ModMenus.SORTER_HOPPER`, `SorterHopperMenu.FILTER_X/SLOT_Y`, `ModBlocks.SORTER_HOPPER`.
- Produces: nothing used by later code.

- [ ] **Step 1: Write the failing loot test**

Append this method inside `SorterHopperGameTest` (before the final `}`):

```java
	@GameTest
	public void breakingDropsSorterHopperItem(GameTestHelper helper) {
		helper.setBlock(SORTER, ModBlocks.SORTER_HOPPER);
		helper.getLevel().destroyBlock(helper.absolutePos(SORTER), true);
		helper.assertItemEntityPresent(ModBlocks.SORTER_HOPPER.asItem(), SORTER, 2.0);
		helper.succeed();
	}
```

- [ ] **Step 2: Run to verify it fails**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runGameTest`
Expected: FAIL: `breakingDropsSorterHopperItem ... Expected item of type Sorter Hopper` (there's no loot table yet, and the name is still the raw key `block.easystorage.sorter_hopper`).

- [ ] **Step 3: Add loot table, recipe, recipe advancement, pickaxe tag**

`src/main/resources/data/easystorage/loot_table/blocks/sorter_hopper.json`:

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1.0,
      "conditions": [
        { "condition": "minecraft:survives_explosion" }
      ],
      "entries": [
        {
          "type": "minecraft:item",
          "name": "easystorage:sorter_hopper",
          "functions": [
            {
              "function": "minecraft:copy_components",
              "source": "block_entity",
              "include": ["minecraft:custom_name"]
            }
          ]
        }
      ]
    }
  ],
  "random_sequence": "easystorage:blocks/sorter_hopper"
}
```

`src/main/resources/data/easystorage/recipe/sorter_hopper.json`:

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "redstone",
  "ingredients": [
    "minecraft:hopper",
    "minecraft:comparator",
    "minecraft:observer"
  ],
  "result": {
    "id": "easystorage:sorter_hopper"
  }
}
```

`src/main/resources/data/easystorage/advancement/recipes/redstone/sorter_hopper.json` (unlocks the recipe book entry when the player gets a hopper):

```json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_hopper": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {
        "items": [
          { "items": "minecraft:hopper" }
        ]
      }
    },
    "has_the_recipe": {
      "trigger": "minecraft:recipe_unlocked",
      "conditions": {
        "recipe": "easystorage:sorter_hopper"
      }
    }
  },
  "requirements": [
    ["has_the_recipe", "has_hopper"]
  ],
  "rewards": {
    "recipes": ["easystorage:sorter_hopper"]
  }
}
```

`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`:

```json
{
  "replace": false,
  "values": ["easystorage:sorter_hopper"]
}
```

- [ ] **Step 4: Run to verify the loot test passes**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runGameTest`
Expected: `All 11 required tests passed :)`.

- [ ] **Step 5: Add blockstate, models, item definition, lang**

Models parent the vanilla hopper models and only swap textures, so the geometry stays identical to vanilla without copying Mojang files.

`src/main/resources/assets/easystorage/blockstates/sorter_hopper.json`:

```json
{
  "variants": {
    "facing=down": { "model": "easystorage:block/sorter_hopper" },
    "facing=north": { "model": "easystorage:block/sorter_hopper_side" },
    "facing=east": { "model": "easystorage:block/sorter_hopper_side", "y": 90 },
    "facing=south": { "model": "easystorage:block/sorter_hopper_side", "y": 180 },
    "facing=west": { "model": "easystorage:block/sorter_hopper_side", "y": 270 }
  }
}
```

`src/main/resources/assets/easystorage/models/block/sorter_hopper.json`:

```json
{
  "parent": "minecraft:block/hopper",
  "textures": {
    "particle": "easystorage:block/sorter_hopper_outside",
    "top": "easystorage:block/sorter_hopper_top",
    "side": "easystorage:block/sorter_hopper_outside",
    "inside": "easystorage:block/sorter_hopper_inside"
  }
}
```

`src/main/resources/assets/easystorage/models/block/sorter_hopper_side.json`:

```json
{
  "parent": "minecraft:block/hopper_side",
  "textures": {
    "particle": "easystorage:block/sorter_hopper_outside",
    "top": "easystorage:block/sorter_hopper_top",
    "side": "easystorage:block/sorter_hopper_outside",
    "inside": "easystorage:block/sorter_hopper_inside"
  }
}
```

`src/main/resources/assets/easystorage/items/sorter_hopper.json` (the item renders as the 3D block model):

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "easystorage:block/sorter_hopper"
  }
}
```

`src/main/resources/assets/easystorage/lang/en_us.json`:

```json
{
  "block.easystorage.sorter_hopper": "Sorter Hopper",
  "container.easystorage.sorter_hopper": "Sorter Hopper"
}
```

- [ ] **Step 6: Generate placeholder textures**

Create `tools/placeholder_textures.py`:

```python
#!/usr/bin/env python3
"""Generate 16x16 placeholder block textures for Easy Storage.

Pure standard library (no Pillow). Output is deterministic, so re-running it
produces identical files. Final art will replace these.

Usage: python3 tools/placeholder_textures.py
"""
import struct
import zlib
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/easystorage/textures/block"

# Blue-tinted iron, matching the filter-slot tint used in the screen (0x3A6EA5).
BASE = (0x3A, 0x6E, 0xA5)
LIGHT = (0x6F, 0x9C, 0xCC)
DARK = (0x22, 0x3F, 0x60)


def png(path, pixels):
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in pixels)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )
    path.write_bytes(data)


def shade(rgb, amount):
    return tuple(max(0, min(255, c + amount)) for c in rgb)


def noise(x, y, seed):
    n = (x * 374761393 + y * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n & 0xF) - 8  # -8..7


def texture(seed, fill, edge_top, edge_bottom):
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            if y == 0 or x == 0:
                rgb = edge_top
            elif y == 15 or x == 15:
                rgb = edge_bottom
            else:
                rgb = shade(fill, noise(x, y, seed))
            row.append(rgb + (255,))
        rows.append(row)
    return rows


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    png(OUT / "sorter_hopper_outside.png", texture(1, BASE, LIGHT, DARK))
    png(OUT / "sorter_hopper_top.png", texture(2, LIGHT, shade(LIGHT, 30), BASE))
    png(OUT / "sorter_hopper_inside.png", texture(3, DARK, BASE, shade(DARK, -20)))
    print(f"wrote placeholders to {OUT}")


if __name__ == "__main__":
    main()
```

Run: `python3 tools/placeholder_textures.py`
Expected: `wrote placeholders to .../textures/block`; `file src/main/resources/assets/easystorage/textures/block/*.png` reports three `PNG image data, 16 x 16` files.

- [ ] **Step 7: Add the screen and register it**

`src/client/java/tr/gokhan/easystorage/client/screen/SorterHopperScreen.java`:

```java
package tr.gokhan.easystorage.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import tr.gokhan.easystorage.menu.SorterHopperMenu;

/**
 * Placeholder screen drawn with flat fills; final art will replace it with a texture.
 */
public class SorterHopperScreen extends AbstractContainerScreen<SorterHopperMenu> {
	private static final int WIDTH = 176;
	private static final int HEIGHT = 133;
	private static final int PANEL = 0xFFC6C6C6;
	private static final int PANEL_LIGHT = 0xFFFFFFFF;
	private static final int PANEL_DARK = 0xFF555555;
	private static final int SLOT_BG = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int FILTER_TINT = 0xFF3A6EA5;
	private static final int ARROW = 0xFF555555;

	public SorterHopperScreen(SorterHopperMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;

		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
		graphics.fill(x, y, x + this.imageWidth - 1, y + this.imageHeight - 1, PANEL_LIGHT);
		graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, PANEL);

		// Tinted frame marks the filter slot.
		int fx = x + SorterHopperMenu.FILTER_X;
		int fy = y + SorterHopperMenu.SLOT_Y;
		graphics.fill(fx - 3, fy - 3, fx + 19, fy + 19, FILTER_TINT);

		// Arrow from filter to buffer.
		int ay = fy + 7;
		graphics.fill(x + 40, ay, x + 54, ay + 2, ARROW);
		graphics.fill(x + 54, ay - 2, x + 56, ay + 4, ARROW);

		for (Slot slot : this.menu.slots) {
			int sx = x + slot.x;
			int sy = y + slot.y;
			graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, PANEL_LIGHT);
			graphics.fill(sx - 1, sy - 1, sx + 16, sy + 16, SLOT_DARK);
			graphics.fill(sx, sy, sx + 16, sy + 16, SLOT_BG);
		}
	}
}
```

Replace `src/client/java/tr/gokhan/easystorage/client/EasyStorageClient.java`:

```java
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
```

- [ ] **Step 8: Build**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew build`
Expected: `BUILD SUCCESSFUL`, `All 11 required tests passed :)`.

- [ ] **Step 9: Manual check in the dev client**

Run: `export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./gradlew runClient`, create a Creative world, then confirm each:
1. The Sorter Hopper appears in the Redstone Blocks tab with the blue placeholder texture (no purple/black missing texture), in hand and placed.
2. Placing it against a block's side makes the spout face that side; placing on top/bottom points it down.
3. Right-click opens the screen: blue-framed filter slot on the left, arrow, 5 buffer slots, player inventory; title "Sorter Hopper".
4. Filter slot takes only 1 item from a stack. Buffer rejects items not matching the filter.
5. Recipe book (survival world, after picking up a hopper) shows the shapeless recipe hopper + comparator + observer.
6. SS3-style line: chest → 3 hoppers in a row pointing east → a hopper at the end into an overflow chest; under each line hopper a Sorter Hopper (filters: diamond, iron ingot, dirt) → chest. Fill the source chest with a mix: every item lands in its slice; others reach the overflow chest; nothing clogs.
7. Quit and reload the world: filter items and buffered items persist.

If any check fails, stop and report; don't patch around it.

- [ ] **Step 10: Commit**

```bash
git add tools src
git commit -m "feat: sorter hopper screen, placeholder art, recipe and loot table"
```

---

### Task 4: Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document the block and tests**

Append to `README.md`:

````markdown
## Blocks

### Sorter Hopper

A hopper that only accepts items matching its filter. Drop-in replacement for the
filter hopper in an SS3 item sorter, without comparators or dummy items.

- Put one item in the filter slot (left). Only that item type is pulled in
  (enchantments, damage and names are ignored).
- Holds up to 5 stacks. Pushes like a hopper, at hopper speed.
- A redstone signal locks it. Comparators read only the 5 buffer slots.
- Recipe (shapeless): hopper + comparator + observer.

## Tests

```sh
./gradlew runGameTest   # in-world GameTests (also run by ./gradlew build)
```

Placeholder textures are generated with `python3 tools/placeholder_textures.py`.
````

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: document Sorter Hopper and tests"
```
