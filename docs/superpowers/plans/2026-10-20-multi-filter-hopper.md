# Multi Filter Hopper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Multi Filter Hopper (27 filter slots) by parameterising the Filter Hopper's classes by filter size, then registering a second block on top.

**Architecture:** `FilterHopperBlockEntity`, `FilterHopperBlock`, `FilterHopperMenu` and `FilterHopperScreen` become size/layout-parameterised; the Filter Hopper keeps filter size 1. `MultiFilterHopper*` classes are thin subclasses that pass size 27, their own ids and layout. Matching becomes "type equals any non-empty filter slot".

**Tech Stack:** Fabric (Loom), Minecraft 26.2 Mojang mappings, Java 25, Fabric GameTest.

**Spec:** `docs/superpowers/specs/2026-10-20-multi-filter-hopper-design.md`

## Global Constraints

- Build/test: `./gradlew build` (Gradle already uses JDK 25 via `~/.gradle/gradle.properties`; runs all GameTests, currently 40 passing).
- Block id `easystorage:multi_filter_hopper`, display name "Multi Filter Hopper", container key `container.easystorage.multi_filter_hopper`.
- Recipe (shapeless, category redstone): `easystorage:filter_hopper` + `minecraft:chest` + `minecraft:diamond`. Advancement unlocks on having `easystorage:filter_hopper`.
- Filter Hopper behavior must not change (all 11 `FilterHopperGameTest` tests stay green unmodified, except imports/signature updates forced by the refactor).
- No new dependencies. Tabs in Java, 2 spaces in JSON. Coloured shulker boxes in 26.2: `Items.DYED_SHULKER_BOX.pick(DyeColor.X)` (if needed).

## Review Focus

1. **Existing Filter Hopper in a saved world** keeps its filter after the save-format change. (Task 1 test `legacyFilterKeyLoadsIntoFirstSlot`.)
2. **Shift-clicking a stack of a new item** into the Multi Filter Hopper moves exactly one into the next empty filter slot and leaves the rest in the inventory, never into the buffer. (Task 2 test `shiftClickStackAddsOneFilter`.)
3. **Breaking the Multi Filter Hopper** drops every filter item, not just slot 0. (Task 2 test `breakingDropsBufferAndAllFilters`.)
4. **Automation never touches filter slots** in the Multi Filter Hopper. (Task 2 test `hopperCannotFillFilters`.)
5. **Full filter + unmatched item shift-click** does nothing (no crash, no loop). (Task 2 test `shiftClickWithFullFilterMovesNothing`.)

---

### Task 1: Parameterise the Filter Hopper by filter size

Refactor only: Filter Hopper behavior is unchanged.

**Files:**
- Modify: `src/main/java/tr/gokhan/easystorage/block/entity/FilterHopperBlockEntity.java`
- Modify: `src/main/java/tr/gokhan/easystorage/block/FilterHopperBlock.java`
- Modify: `src/main/java/tr/gokhan/easystorage/menu/FilterHopperMenu.java`
- Modify: `src/client/java/tr/gokhan/easystorage/client/screen/FilterHopperScreen.java`, `src/client/java/tr/gokhan/easystorage/client/EasyStorageClient.java` (generic screen registration)
- Test: `src/gametest/java/tr/gokhan/easystorage/test/FilterHopperGameTest.java`

**Interfaces (Produces):**
- `FilterHopperBlockEntity`:
  - `protected FilterHopperBlockEntity(BlockEntityType<? extends FilterHopperBlockEntity> type, BlockPos pos, BlockState state, int filterSize)`; the public `(BlockPos, BlockState)` constructor delegates with `ModBlockEntities.FILTER_HOPPER` and `1`.
  - `public Container getFilter()` (size `filterSize`, max stack 1, `setChanged` propagates as today).
  - Keep `getFilterItem()` / `setFilterItem(ItemStack)` (slot 0); add `public void setFilterItem(int slot, ItemStack stack)`.
  - Replace `matchesFilter(ItemStack, ItemStack)` with `public static boolean matchesFilter(Container filter, ItemStack stack)`: true when any non-empty filter slot has the same item type.
  - Save the filter as `output.store("filters", ItemContainerContents.CODEC, ItemContainerContents.fromItems(...))`; load from `"filters"` (copyInto), else fall back to the legacy single `"filter"` ItemStack into slot 0.
  - `createMenu` / `getDefaultName` stay overridable (subclass supplies its own).
- `FilterHopperBlock`:
  - `protected BlockEntityType<? extends FilterHopperBlockEntity> blockEntityType()` returning `ModBlockEntities.FILTER_HOPPER`; `getTicker` uses it.
  - `codec()` return type widened to `MapCodec<? extends FilterHopperBlock>` so a subclass can return its own codec.
  - `useWithoutItem` / `entityInside` / comparator already work on `FilterHopperBlockEntity` instances, so keep them.
- `FilterHopperMenu`:
  - `protected FilterHopperMenu(MenuType<?> type, int containerId, Inventory inventory, Container buffer, Container filter, int filterX, int filterY, int filterColumns, int bufferX, int bufferY, int inventoryY)`. Filter slots are menu indices `0..filterSize-1` laid out in a grid of `filterColumns` at 18 px pitch, then 5 buffer slots, then the player inventory.
  - Public constructors keep today's signatures and values (`FILTER_X = 17`, `BUFFER_X = 62`, `SLOT_Y = 20`, inventory y `51`, 1 column). Static `FILTER_SLOT`, `BUFFER_START`, `BUFFER_END` stay for the Filter Hopper.
  - `public int filterSize()`, `public boolean isFilterSlot(Slot slot)`, `public int screenHeight()` (Filter Hopper: `133`).
  - Shared shift-click rule from the player inventory: matches filter → buffer; else, if a filter slot is empty → exactly one into the first empty filter slot and return `ItemStack.EMPTY` (the existing comment explains why); else → nothing. Loader slots (filter/buffer) → player inventory.
  - `BufferSlot.mayPlace` and `canTakeItemForPickAll` use `matchesFilter(filter, stack)` / `isFilterSlot`.
- `FilterHopperScreen<T extends FilterHopperMenu> extends AbstractContainerScreen<T>`: public `(T, Inventory, Component)` constructor; height from `menu.screenHeight()`; tinted frame drawn around every filter slot; the arrow only when `menu.filterSize() == 1`; inventory label at `imageHeight - 94` as today.

- [ ] **Step 1: Write the failing test** `legacyFilterKeyLoadsIntoFirstSlot` in `FilterHopperGameTest`: build a `CompoundTag` with `"filter"` = diamond (encode via `ItemStack.CODEC` with the level's registry ops), load it into a fresh `FilterHopperBlockEntity` with `loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag))`; assert `getFilterItem().is(Items.DIAMOND)`. Also `saveLoadRoundTripKeepsFilter`: set filter, `saveWithoutMetadata`, load into a fresh entity, assert filter kept.

- [ ] **Step 2: Run on the unrefactored code, verify both pass, commit the tests**

Run: `./gradlew build`
Expected: "All 42 required tests passed". These tests pin today's save format, so they must be green before the refactor (refactor task: no RED by design). Commit: `git commit -m "test: pin Filter Hopper filter save format"`.

- [ ] **Step 3: Refactor** per Interfaces.

- [ ] **Step 4: Run, verify all pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, "All 42 required tests passed".

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "refactor: parameterise Filter Hopper by filter size"
```

---

### Task 2: Multi Filter Hopper block

**Files:**
- Create: `src/main/java/tr/gokhan/easystorage/block/entity/MultiFilterHopperBlockEntity.java` (`FILTER_SIZE = 27`, default name key `container.easystorage.multi_filter_hopper`, `createMenu` → `MultiFilterHopperMenu`)
- Create: `src/main/java/tr/gokhan/easystorage/block/MultiFilterHopperBlock.java` (own `CODEC`, `newBlockEntity`, `blockEntityType()` → `ModBlockEntities.MULTI_FILTER_HOPPER`)
- Create: `src/main/java/tr/gokhan/easystorage/menu/MultiFilterHopperMenu.java`
- Modify: `ModBlocks` (`MULTI_FILTER_HOPPER`, same properties as `FILTER_HOPPER`; creative tab after `FILTER_HOPPER`), `ModBlockEntities`, `ModMenus`, `EasyStorageClient` (register `FilterHopperScreen::new` for the new menu)
- Create: `src/gametest/java/tr/gokhan/easystorage/test/MultiFilterHopperGameTest.java`
- Modify: `src/gametest/resources/fabric.mod.json` (add entrypoint)

**Interfaces:**
- Consumes: Task 1's protected constructors, `blockEntityType()`, `matchesFilter(Container, ItemStack)`, `setFilterItem(int, ItemStack)`, menu layout constructor.
- Produces `MultiFilterHopperMenu`: `public MultiFilterHopperMenu(int containerId, Inventory inventory)` (client: `SimpleContainer(BUFFER_SIZE)`, `SimpleContainer(27)`), `public MultiFilterHopperMenu(int containerId, Inventory inventory, Container buffer, Container filter)`. Layout: filter grid at `(8, 18)`, 9 columns (rows y 18/36/54); buffer at `(44, 80)`; inventory at y `111`; `screenHeight() = 193`. Static `BUFFER_START = 27`, `BUFFER_END = 32`.

- [ ] **Step 1: Write the failing tests** in `MultiFilterHopperGameTest` (column helper like `FilterHopperGameTest`: chest above, Multi Filter Hopper facing down, chest below):
  - `anyFilterItemIsAccepted`: filters diamond (slot 0) and emerald (slot 13); top chest holds diamond and emerald; `succeedWhen` bottom chest contains both.
  - `nonMatchingItemContinuesAlongLine`: line-hopper layout from the Filter Hopper test, filters diamond + emerald; source has diamond, emerald, dirt; diamond and emerald sorted, dirt in overflow and never in the sorted chest.
  - `emptyFilterAcceptsNothing`, `redstoneSignalLocksTransfers`: as in `FilterHopperGameTest`.
  - `comparatorIgnoresFilters`: 27 filters filled, buffer empty → signal 0; one buffered item → `> 0`.
  - `breakingDropsBufferAndAllFilters`: filters diamond (slot 0), emerald (slot 26), buffer 3 gold ingots; destroy; item entities: 1 diamond, 1 emerald, 3 gold.
  - `hopperCannotFillFilters`: vanilla hopper above holds dirt, filter has diamond in slot 0; after 20 ticks the filter container still has only the diamond and the buffer is empty.
  - `shiftClickStackAddsOneFilter`: mock player, menu over the block entity; player slot holds 5 dirt; quick-move → filter slot 0 holds 1 dirt, player keeps 4, buffer 0. Quick-move again → buffer holds 4 dirt.
  - `shiftClickWithFullFilterMovesNothing`: 27 filter slots filled with stone; quick-move 3 dirt → player keeps 3, buffer 0.
  - `breakingDropsMultiFilterHopperItem`: lives in Task 3 (needs the loot table).

- [ ] **Step 2: Run, verify failure**

Run: `./gradlew build`
Expected: compile failure (`MultiFilterHopperBlockEntity` not found).

- [ ] **Step 3: Implement** the files above.

- [ ] **Step 4: Run, verify all pass**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL, all GameTests pass (42 + Task 2's).

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: Multi Filter Hopper block"
```

---

### Task 3: Assets, data, docs

**Files:**
- Modify: `tools/placeholder_textures.py`: add `multi_filter_hopper_outside/top/inside.png` with the same `texture()` calls as the Filter Hopper but a purple palette (`MULTI_BASE = (0x7A, 0x3E, 0xA5)` plus light/dark shades). Filter Hopper and Shulker Loader PNGs must stay byte-identical.
- Create: `assets/easystorage/blockstates/multi_filter_hopper.json`, `models/block/multi_filter_hopper.json`, `models/block/multi_filter_hopper_side.json`, `models/item/multi_filter_hopper.json`, `items/multi_filter_hopper.json`: copies of the Filter Hopper's with ids/textures renamed.
- Modify: `lang/en_us.json` (block + container keys → "Multi Filter Hopper").
- Create: `data/easystorage/recipe/multi_filter_hopper.json`, `data/easystorage/advancement/recipes/redstone/multi_filter_hopper.json` (criterion `has_filter_hopper` on item `easystorage:filter_hopper`), `data/easystorage/loot_table/blocks/multi_filter_hopper.json` (copy of the Filter Hopper's).
- Modify: `data/minecraft/tags/block/mineable/pickaxe.json`.
- Modify: `README.md` (new "### Multi Filter Hopper" section after the Filter Hopper, with recipe line), `docs/TODO.md` (final textures; filter display on top applies to both).
- Test: `MultiFilterHopperGameTest`

- [ ] **Step 1: Write the failing test** `breakingDropsMultiFilterHopperItem`: place, destroy, `assertItemEntityPresent(ModBlocks.MULTI_FILTER_HOPPER.asItem(), pos, 2.0)`.
- [ ] **Step 2: Run, verify failure.** Run: `./gradlew build`. Expected: FAIL in `breakingDropsMultiFilterHopperItem`.
- [ ] **Step 3: Run `python3 tools/placeholder_textures.py` and add the files.** `git status` must show only new PNGs under textures.
- [ ] **Step 4: Run, verify.** Run: `./gradlew build 2>&1 | grep -iE "missing|multi_filter_hopper|tests passed"`. Expected: all tests pass, no missing-asset lines for `multi_filter_hopper`.
- [ ] **Step 5: Commit**

```bash
git add tools src README.md docs/TODO.md
git commit -m "feat: Multi Filter Hopper assets, recipe, loot table and docs"
```
