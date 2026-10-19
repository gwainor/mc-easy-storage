# Shulker Loader Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Shulker Loader: a dropper-like block that loads one shulker box from automation, fills it, and drops it when full or on a redstone pulse.

**Architecture:** One `BaseContainerBlockEntity` exposing 28 slots as a `WorldlyContainer`: slot 0 is the box, slots 1–27 are the box's contents. The content list is the live truth while a box is loaded; every `setChanged()` writes it back into the box's `CONTAINER` component, so whatever path moves the box (drop, hopper, menu, break) carries its contents. Slot-0 changes load contents from the box (box in) or clear them (box out). Redstone mirrors the vanilla dispenser's `TRIGGERED` property; ticking handles drops.

**Tech Stack:** Fabric (Loom), Minecraft 26.2 Mojang mappings, Java 25, Fabric GameTest.

**Spec:** `docs/superpowers/specs/2026-10-20-shulker-loader-design.md`

## Global Constraints

- Build/test with `export JAVA_HOME=/opt/homebrew/opt/openjdk@25` then `./gradlew build` (runs GameTests). Single test class: `./gradlew runGameTest`.
- Block id `easystorage:shulker_loader`, display name "Shulker Loader".
- No new dependencies. Tabs for Java indentation, 2 spaces in JSON. Follow the Filter Hopper's structure and idioms (`FilterHopperBlock`, `FilterHopperBlockEntity`, `FilterHopperMenu`, `FilterHopperScreen`, `FilterHopperGameTest`).
- Box detection: `stack.is(ItemTags.SHULKER_BOXES)`. Nesting rule for contents: `stack.getItem().canFitInsideContainerItems()`.
- Box slot holds exactly one box regardless of the item's max stack size (Carpet-style stacking).
- Vanilla reference for 26.2 APIs: the removed Item Feeder (`git show 2cd180b^:src/main/java/tr/gokhan/easystorage/block/ItemFeederBlock.java`, `.../block/entity/ItemFeederBlockEntity.java`, `.../block/entity/ShulkerUnloader.java`) has working FACING placement, `CONTAINER` component read/write, and blockstate JSON for 6 facings. Vanilla `DropperBlock#dispenseFrom` is the drop reference.

## Review Focus

1. **Shift-clicking a filled box out of the screen** must move the box *with* its contents and leave the grid empty: no loss, no duplication. (Task 3 test `shiftClickBoxOutKeepsContents`.)
2. **Breaking the block** must drop the Loader + one box holding the contents, and must not also spill the 27 content slots (vanilla `BaseContainerBlockEntity` would drop all 28 slots). (Task 1 test `breakingDropsBoxWithContentsOnly`.)
3. **A stack of stackable boxes offered directly** (another mod calling `HopperBlockEntity.addItem` with count > 1) must not delete the extras: vanilla `tryMoveInItem` puts the whole stack into an empty slot. Refuse multi-count box offers through automation. (Task 1 test `multiCountBoxOfferIsRefused`.)
4. **Chunk unload / world save mid-fill** must keep the box, its contents and a pending drop. (Task 1 test `saveLoadKeepsBoxContents`; Task 2 test `saveLoadKeepsPendingDrop`.)
5. **Hopper below drains while a hopper above fills**, and the drained box stays put, never pulled itself. (Task 2 test `hopperBelowPullsContentsNotBox`.)

---

### Task 1: Block entity inventory model, block, registration

**Files:**
- Create: `src/main/java/tr/gokhan/easystorage/block/entity/ShulkerLoaderBlockEntity.java`
- Create: `src/main/java/tr/gokhan/easystorage/block/ShulkerLoaderBlock.java`
- Modify: `src/main/java/tr/gokhan/easystorage/registry/ModBlocks.java` (add `SHULKER_LOADER`)
- Modify: `src/main/java/tr/gokhan/easystorage/registry/ModBlockEntities.java` (add `SHULKER_LOADER`)
- Create: `src/gametest/java/tr/gokhan/easystorage/test/ShulkerLoaderGameTest.java`
- Modify: `src/gametest/resources/fabric.mod.json` (add `tr.gokhan.easystorage.test.ShulkerLoaderGameTest` entrypoint)

**Interfaces:**
- Produces, in `ShulkerLoaderBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer`:
  - `public static final int BOX_SLOT = 0, CONTENT_START = 1, CONTENT_SIZE = 27, CONTAINER_SIZE = 28;`
  - `public static boolean isShulkerBox(ItemStack stack)`
  - `public boolean hasBox()`, `public ItemStack getBox()`
  - `public boolean isContentFull()`: every content slot holds a stack at its max size.
  - `public boolean fits(ItemStack stack)`: an empty content slot exists, or a same-item-same-components part stack has room.
  - `public int getAnalogSignal()`: vanilla `getRedstoneSignalFromContainer` formula over slots 1–27 only (0 with no box).
  - Container name key `container.easystorage.shulker_loader`. Until Task 3, `createMenu` throws `UnsupportedOperationException` and the block has no `useWithoutItem` override.
- Produces, in `ShulkerLoaderBlock extends BaseEntityBlock`:
  - `public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;`
  - `public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;` (true while powered = locked)
  - Placement: `FACING = context.getNearestLookingDirection().getOpposite()`, `TRIGGERED = false`. Comparator via `getAnalogSignal()`. `rotate`/`mirror` like the Item Feeder.
- Produces `ModBlocks.SHULKER_LOADER` (properties copied from vanilla dropper: `MapColor.STONE`, `NoteBlockInstrument.BASEDRUM`, `requiresCorrectToolForDrops()`, `strength(3.5F)`), `ModBlockEntities.SHULKER_LOADER`.

Inventory rules the block entity must implement:
- `setItem(BOX_SLOT, box)`: store the box, copy its `CONTAINER` contents into slots 1–27.
- `setItem(BOX_SLOT, EMPTY)`, `removeItem(BOX_SLOT, n)`, `removeItemNoUpdate(BOX_SLOT)`: return the box (already carrying contents), clear slots 1–27.
- `setChanged()`: if a box is loaded, write slots 1–27 into its `CONTAINER` component (`ItemContainerContents.fromItems`), then `super.setChanged()`.
- `getMaxStackSize(ItemStack stack)`: 1 for shulker boxes, else `super`.
- `canPlaceItem(slot, stack)`: slot 0 → box, count 1, no box loaded; content slots → box loaded and `canFitInsideContainerItems()`.
- `getSlotsForFace(any)`: all 28. `canTakeItemThroughFace`: content slots only.
- `canPlaceItemThroughFace`: `canPlaceItem` rules only in this task (lock/pending/fit rules arrive in Task 2).
- Save only the box under key `"box"` (call the write-back first); load restores contents from it. Override `preRemoveSideEffects` to drop only the box (`Containers.dropItemStack`), not `super`'s all-slots drop.

- [ ] **Step 1: Write the failing tests** in `ShulkerLoaderGameTest` (layout helper: loader at `(1,2,1)`, optional chest/hopper at `(1,3,1)` above):
  - `hopperLoadsOneBoxOnly`: hopper above (facing down) holds `SHULKER_BOX` and `RED_SHULKER_BOX`; after 40 ticks loader `getBox().is(Items.SHULKER_BOX)` and the hopper still holds the red box.
  - `stackedBoxesLoadOne`: hopper above holds 3 `SHULKER_BOX` with `DataComponents.MAX_STACK_SIZE` set to 64; after 40 ticks loader has a box of count 1 and the hopper holds 2.
  - `multiCountBoxOfferIsRefused`: `HopperBlockEntity.addItem(null, loader, <3 stackable boxes>, Direction.UP)` returns a stack of count 3 and `hasBox()` is false.
  - `itemsRefusedWithoutBox`: hopper above holds 1 diamond; after 20 ticks the loader is empty and the hopper still holds the diamond.
  - `itemsGoIntoLoadedBox`: `loader.setItem(BOX_SLOT, new ItemStack(Items.SHULKER_BOX))`, hopper above holds 5 diamonds; `succeedWhen` `loader.countItem(Items.DIAMOND) == 5`, and `getBox()`'s `CONTAINER` holds 5 diamonds.
  - `boxNeverGoesIntoContents`: with a box loaded, `canPlaceItem(CONTENT_START, new ItemStack(Items.BLUE_SHULKER_BOX))` is false.
  - `prefilledBoxShowsContents`: load a box whose `CONTAINER` holds 10 dirt in slot 4; `loader.getItem(CONTENT_START + 4)` is 10 dirt.
  - `takingBoxCarriesContentsAndClears`: load empty box, `setItem(CONTENT_START, 7 diamonds)`, `setChanged()`, `removeItem(BOX_SLOT, 1)` returns a box whose `CONTAINER` holds 7 diamonds; afterwards `loader.isEmpty()`.
  - `breakingDropsBoxWithContentsOnly`: load box + 7 diamonds, `destroyBlock(..., true)`; item entities within 2 blocks: exactly 1 `SHULKER_BOX` (its `CONTAINER` holds 7 diamonds), 0 loose diamonds.
  - `comparatorReadsContentsOnly`: box alone → `getAnalogSignal() == 0`; after adding 1 diamond → `> 0`.
  - `saveLoadKeepsBoxContents`: load box + 7 diamonds, `saveWithoutMetadata(registries)`, load into a fresh `ShulkerLoaderBlockEntity` via `loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag))`; fresh entity has the box and `countItem(Items.DIAMOND) == 7`.

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew build`
Expected: compile failure (`ShulkerLoaderBlockEntity` not found).

- [ ] **Step 3: Implement `ShulkerLoaderBlockEntity`, `ShulkerLoaderBlock`, registrations, test entrypoint** per the Interfaces and rules above.

- [ ] **Step 4: Run tests, verify they pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, "All N required tests passed" (11 existing + Task 1 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java src/gametest
git commit -m "feat: Shulker Loader block entity and inventory model"
```

---

### Task 2: Drops, redstone, automation lock

**Files:**
- Modify: `src/main/java/tr/gokhan/easystorage/block/entity/ShulkerLoaderBlockEntity.java`
- Modify: `src/main/java/tr/gokhan/easystorage/block/ShulkerLoaderBlock.java`
- Test: `src/gametest/java/tr/gokhan/easystorage/test/ShulkerLoaderGameTest.java`

**Interfaces:**
- Consumes: Task 1 block entity API, `ShulkerLoaderBlock.FACING` / `TRIGGERED`.
- Produces on the block entity:
  - `public void requestDrop()`: sets the pending flag if a box is loaded.
  - `public boolean isDropPending()`
  - `public static void serverTick(Level level, BlockPos pos, BlockState state, ShulkerLoaderBlockEntity loader)`, registered as the ticker (server only).
  - Pending flag saved as `"PendingDrop"`.

Behavior:
- Block `neighborChanged`/`onPlace`: `powered = level.hasNeighborSignal(pos)` (no quasi-connectivity). Rising edge (`powered && !TRIGGERED`) → set `TRIGGERED` true and `requestDrop()`. Falling → `TRIGGERED` false. `onPlace` syncs `TRIGGERED` without requesting a drop.
- `serverTick`: no box → clear pending, return. If pending, or (`!TRIGGERED` and `isContentFull()`), try to drop; on failure set pending.
- Drop: `into = HopperBlockEntity.getContainerAt(level, pos.relative(facing))`. With a container: `HopperBlockEntity.addItem(this, into, getBox().copy(), facing.getOpposite())`; success when the result is empty. Without: `DefaultDispenseItemBehavior.spawnItem(level, box.copy(), 6, facing, position)` where `position = Vec3.atCenterOf(pos)` offset `0.7` along `facing` (same as `DispenserBlock.getDispensePosition`), plus `level.levelEvent(1000, pos, 0)` and `level.levelEvent(2000, pos, facing.get3DDataValue())`. On success clear the box slot (contents left with the box) and the pending flag.
- `canPlaceItemThroughFace` adds: refuse everything when `TRIGGERED` or pending. For a non-box offer to a content slot that passes `canPlaceItem` but `!fits(stack)`: refuse and `requestDrop()`. An offered box never triggers a drop.
- `canTakeItemThroughFace` is not affected by the lock.

- [ ] **Step 1: Write the failing tests** (loader facing `EAST` at `(1,2,1)` unless noted; "front" = `(2,2,1)`):
  - `fullBoxDropsIntoChestInFront`: chest in front; load box, fill 27 slots with 64 cobblestone, `setChanged()`; `succeedWhen` chest contains a `SHULKER_BOX` whose `CONTAINER` totals 1728 cobblestone and `!loader.hasBox()`.
  - `fullBoxDropsAsItemEntity`: air in front; same fill; `succeedWhen` an item entity with a `SHULKER_BOX` is within 2 blocks of front and `!loader.hasBox()`.
  - `itemThatDoesNotFitDropsBox`: chest in front; load a box whose 27 slots each hold 1 dirt (all occupied, none full); hopper above with 1 diamond. `succeedWhen` the chest holds the box and the hopper still holds the diamond. Then `loader.setItem(BOX_SLOT, new ItemStack(Items.SHULKER_BOX))` and `succeedWhen` `loader.countItem(Items.DIAMOND) == 1`.
  - `offeredBoxDoesNotTriggerDrop`: box loaded with 1 dirt, hopper above holds a red box; after 20 ticks `hasBox()` still true, no drop pending.
  - `redstonePulseDropsPartialBox`: chest in front; box with 3 diamonds; place `REDSTONE_BLOCK` at `(1,2,2)`; `succeedWhen` chest holds the box with 3 diamonds.
  - `poweredLoaderRefusesInput`: redstone block placed, then hopper above with a box and a diamond; after 30 ticks loader empty, `TRIGGERED` true, hopper holds both.
  - `constantSignalDoesNotRefire`: powered, box drops; then `loader.setItem(BOX_SLOT, box)` while still powered; after 20 ticks `hasBox()` still true.
  - `blockedDropStaysPending`: chest in front filled with 27 stone stacks; pulse; after 10 ticks `isDropPending()` and `hasBox()`; then clear one chest slot; `succeedWhen` chest holds the box.
  - `hopperBelowPullsContentsNotBox`: loader facing `EAST`, hopper below at `(1,1,1)` facing down into a chest at `(1,0,1)`; box with 3 diamonds; `succeedWhen` the bottom chest holds 3 diamonds, `hasBox()` still true, no shulker box in the chest.
  - `hopperBelowWorksWhileLocked`: same layout; place a redstone block at `(1,2,2)`, wait 5 ticks (box-less pulse drops nothing), then `setItem(BOX_SLOT, box with 3 diamonds)`; `succeedWhen` the bottom chest holds 3 diamonds and `hasBox()` is still true. (`(1,2,2)` is not adjacent to the hopper at `(1,1,1)`, so the hopper stays unlocked.)
  - `saveLoadKeepsPendingDrop`: chest in front full, pulse, wait until pending; round-trip as in Task 1; fresh entity `isDropPending()`.

- [ ] **Step 2: Run tests, verify the new ones fail**

Run: `./gradlew build`
Expected: compile failure (`requestDrop` / `isDropPending` missing).

- [ ] **Step 3: Implement** the behavior above.

- [ ] **Step 4: Run tests, verify all pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all GameTests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java src/gametest
git commit -m "feat: Shulker Loader drops, redstone pulse and lock"
```

---

### Task 3: Menu and screen

**Files:**
- Create: `src/main/java/tr/gokhan/easystorage/menu/ShulkerLoaderMenu.java`
- Modify: `src/main/java/tr/gokhan/easystorage/registry/ModMenus.java` (add `SHULKER_LOADER`)
- Modify: `ShulkerLoaderBlockEntity#createMenu`, `ShulkerLoaderBlock#useWithoutItem` (open menu like the Filter Hopper)
- Create: `src/client/java/tr/gokhan/easystorage/client/screen/ShulkerLoaderScreen.java`
- Modify: `src/client/java/tr/gokhan/easystorage/client/EasyStorageClient.java`
- Test: `src/gametest/java/tr/gokhan/easystorage/test/ShulkerLoaderGameTest.java`

**Interfaces:**
- Consumes: Task 1 slot constants, `isShulkerBox`, `hasBox`.
- Produces `ShulkerLoaderMenu extends AbstractContainerMenu`:
  - `public ShulkerLoaderMenu(int containerId, Inventory inventory)` (client, `new SimpleContainer(CONTAINER_SIZE)`), `public ShulkerLoaderMenu(int containerId, Inventory inventory, Container loader)`.
  - Menu slot indices equal container indices: `BOX_SLOT = 0`, `CONTENT_START = 1`, `CONTENT_END = 28`, then player inventory (28–54) and hotbar (55–63).
  - Positions: `BOX_X = 80, BOX_Y = 20`, grid `GRID_X = 8, GRID_Y = 46` (3×9, 18 px pitch), `addStandardInventorySlots(inventory, 8, 118)`. Screen image 176×200.
- Slot rules: box slot `getMaxStackSize() == 1`, `mayPlace` = `isShulkerBox`. Content slots `mayPlace` = box present (read slot 0 of the menu's container, so it works client-side) and `canFitInsideContainerItems()`; `mayPickup` always true.
- `quickMoveStack`: loader slots → player inventory. From player: a shulker box with no box loaded → box slot, exactly one, return `ItemStack.EMPTY` (same reason as `FilterHopperMenu`'s filter-slot branch); other items with a box loaded → content slots; else nothing.
- `canTakeItemForPickAll` false for the box slot.
- Screen: Filter Hopper style flat fills; tinted frame around the box slot; when the box slot is empty, draw a grey overlay over the content grid.

- [ ] **Step 1: Write the failing tests** (mock player like `shiftClickFillsFilterThenBuffer`):
  - `shiftClickLoadsOneBox`: player slot holds 3 stackable boxes (MAX_STACK_SIZE 64); quick-move → loader has 1 box, player keeps 2.
  - `shiftClickItemsNeedBox`: no box; quick-move 5 diamonds → loader empty, player keeps 5. Load box; quick-move again → `countItem(DIAMOND) == 5`.
  - `shiftClickBoxOutKeepsContents`: box + 7 diamonds loaded; quick-move menu slot 0 → player inventory holds one box whose `CONTAINER` totals 7 diamonds; `loader.isEmpty()`; no loose diamonds in the player inventory.
  - `contentSlotsLockedWithoutBox`: `menu.getSlot(CONTENT_START).mayPlace(new ItemStack(Items.DIAMOND))` false with no box, true with a box; `mayPlace` of a shulker box on a content slot false.

- [ ] **Step 2: Run tests, verify they fail**

Run: `./gradlew build`
Expected: compile failure (`ShulkerLoaderMenu` not found).

- [ ] **Step 3: Implement** menu, registration, block-entity/block wiring, screen, client registration.

- [ ] **Step 4: Run tests, verify all pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all GameTests pass.

- [ ] **Step 5: Manual check in the dev client**

Run: `./gradlew runClient`, place the Loader, open it: box slot and grey grid shown; insert a box by hand, grid ungreys; take it out, contents go with it.

- [ ] **Step 6: Commit**

```bash
git add src/main/java src/client src/gametest
git commit -m "feat: Shulker Loader screen"
```

---

### Task 4: Assets, data, docs

**Files:**
- Modify: `tools/placeholder_textures.py` (add `shulker_loader_front.png` with an orange-ringed hole, `shulker_loader_side.png`, `shulker_loader_back.png`; reuse `texture()`/`shade()`/`noise()`, add an orange constant)
- Create: `src/main/resources/assets/easystorage/blockstates/shulker_loader.json` (6 facings × `triggered` = 12 variants, same model; rotations as the old Item Feeder blockstate)
- Create: `src/main/resources/assets/easystorage/models/block/shulker_loader.json` (`minecraft:block/cube_bottom_top`, top = front)
- Create: `src/main/resources/assets/easystorage/items/shulker_loader.json`
- Modify: `src/main/resources/assets/easystorage/lang/en_us.json` (`block.easystorage.shulker_loader`, `container.easystorage.shulker_loader` → "Shulker Loader")
- Create: `src/main/resources/data/easystorage/recipe/shulker_loader.json` (shapeless, category redstone: `minecraft:dropper`, `minecraft:hopper`, `minecraft:shulker_shell`)
- Create: `src/main/resources/data/easystorage/advancement/recipes/redstone/shulker_loader.json` (unlock on `minecraft:shulker_shell`, Filter Hopper's shape)
- Create: `src/main/resources/data/easystorage/loot_table/blocks/shulker_loader.json` (copy of the Filter Hopper's with the new id)
- Modify: `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json` (add `easystorage:shulker_loader`)
- Modify: `ModBlocks.initialize()` (creative tab: accept `SHULKER_LOADER` after `FILTER_HOPPER`)
- Modify: `README.md` (new "### Shulker Loader" section under Blocks, intro mentions shulker box loading), `docs/TODO.md` (new "## Shulker Loader" section: final textures and screen art)
- Test: `src/gametest/java/tr/gokhan/easystorage/test/ShulkerLoaderGameTest.java`

- [ ] **Step 1: Write the failing test** `breakingDropsShulkerLoaderItem`: place, `destroyBlock(..., true)`, `assertItemEntityPresent(ModBlocks.SHULKER_LOADER.asItem(), pos, 2.0)`.

- [ ] **Step 2: Run, verify it fails**

Run: `./gradlew build`
Expected: FAIL in `breakingDropsShulkerLoaderItem` (no loot table).

- [ ] **Step 3: Generate textures and add all files above**

Run: `python3 tools/placeholder_textures.py` (existing Filter Hopper PNGs must come out byte-identical: `git diff --stat src/main/resources/assets/easystorage/textures` shows only new files).

- [ ] **Step 4: Run, verify all pass and no missing-asset warnings**

Run: `./gradlew build 2>&1 | grep -iE "missing|shulker_loader|tests passed"`
Expected: "All N required tests passed", no missing model/texture lines for `shulker_loader`.

- [ ] **Step 5: Commit**

```bash
git add tools src/main/resources src/main/java README.md docs/TODO.md src/gametest
git commit -m "feat: Shulker Loader assets, recipe, loot table and docs"
```
