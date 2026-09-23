# Sorter Hopper — Design

- **Date:** 2026-09-23
- **Mod:** Easy Storage (`easystorage`), Fabric, Minecraft 26.2, Java 25
- **Status:** Approved in brainstorming, pending spec review

## Goal

Replace the vanilla "SS3" item sorter slice (filter hopper + comparator + redstone
torch + dummy items) with a single block: the **Sorter Hopper**. It behaves like a
vanilla hopper but only accepts items matching a filter item, holds up to 5 stacks
of that item, and needs no redstone.

## Background

A vanilla SS3 sorter slice loads a filter hopper with filter items plus renamed
dummy items so a comparator outputs signal strength 3. It buffers ~23 items, is
fiddly to build, and breaks or overflows when its chest fills. The Sorter Hopper
drops into the same position under a hopper line and removes all the redstone.

## Behavior

### Placement and orientation
- Placed like a vanilla hopper: `FACING` is `DOWN` or one of the 4 horizontal
  directions, taken from the clicked face.
- `ENABLED` block state property, same semantics as vanilla: a redstone signal
  disables (locks) it.

### Slots
- **Buffer:** 5 slots. This is the only inventory automation sees.
- **Filter:** 1 separate slot holding a real item (not a ghost copy), max stack
  size 1. Only the player can change it through the screen.

### Filter matching
- Match by item type only: `stack.is(filter.getItem())`. Components
  (enchantments, damage, custom name, etc.) are ignored.
- Works with unstackable items (e.g. an iron sword filter accepts any iron sword).
- Each shulker box colour is a distinct item type and is matched separately.
- Empty filter → accepts nothing.
- Changing the filter does not touch items already in the buffer; they continue
  to be pushed out. Only new intake uses the new filter.

### Transfer
- Same timing as a vanilla hopper: one transfer attempt per 8 game ticks
  (`HopperBlockEntity.MOVE_ITEM_SPEED`), including vanilla cooldown behaviour.
- Each tick cycle, when not locked by redstone:
  1. **Push** one item from the buffer into the container in `FACING` direction.
  2. **Pull** one matching item from the container above, or, if there is no
     container above, from item entities resting on/above the block.
- Filtering is enforced through `canPlaceItem(slot, stack)` on the buffer
  container. Vanilla helpers (`suckInItems`, `addItem`) call this, so the same
  single check filters pulls from above, pushes from other hoppers, and item
  entities.
- Other hoppers / hopper minecarts below may pull from the buffer (allows
  chaining).

### Comparator
- Comparator output is computed from the 5 buffer slots only, with the vanilla
  container formula. The filter item never contributes.

### Breaking
- Drops all buffer contents and the filter item.
- The block drops as an empty item (no content retention), like the vanilla
  hopper.

### Recipe
- Shapeless: 1 hopper + 1 comparator + 1 observer → 1 Sorter Hopper.
- Recipe book entry unlocked when the player obtains a hopper.

## Screen

Same size as the vanilla hopper screen.

```
┌───────────────────────────────────┐
│ Sorter Hopper                     │
│  Filter      Buffer               │
│  [F]  ▸   [1][2][3][4][5]          │
│                                   │
│ Inventory (3 rows + hotbar)       │
└───────────────────────────────────┘
```

- Filter slot on the left with its own tinted frame and an arrow toward the
  buffer row.
- Title uses the block's custom name when renamed in an anvil.

### Slot rules
- **Filter slot:** accepts any item, max 1. Placing a stack inserts 1, the rest
  stays on the cursor.
- **Buffer slots:** player can take freely; player can insert only items
  matching the filter.
- **Shift-click from player inventory:** filter empty → 1 item into filter;
  else if matches filter → buffer; else nothing.
- **Shift-click from filter or buffer:** → player inventory.

## Architecture

All code under `tr.gokhan.easystorage`.

| Unit | Responsibility |
|---|---|
| `block/SorterHopperBlock` | Block: `FACING`, `ENABLED`, shape (vanilla hopper shape), placement, redstone updates, opening the menu, drops on removal, comparator output, ticker hookup. |
| `block/entity/SorterHopperBlockEntity` | Buffer (5) + filter (1) storage, `canPlaceItem` filter rule, tick logic (push/pull/cooldown) reusing public static helpers in vanilla `HopperBlockEntity` where available, NBT save/load. Implements vanilla `Hopper` and `Container` (buffer only). |
| `menu/SorterHopperMenu` | Server menu: filter slot, 5 buffer slots, player inventory, shift-click rules. |
| `client/screen/SorterHopperScreen` | Client screen rendering. |
| `registry/ModBlocks`, `ModBlockEntities`, `ModItems`, `ModMenus` | Registration. |

Approach rationale: an independent block + block entity avoids mixins and access
wideners on vanilla hopper internals, which are fragile across versions (26.2 →
26.3 → 26.4), and does not change vanilla hoppers.

### Persistence
- Buffer saved with vanilla container helpers.
- Filter saved under a separate `filter` key.
- Transfer cooldown and custom name saved as vanilla does.

### Compatibility
- Vanilla hoppers, droppers, hopper minecarts and comparators interact natively.
- Fabric Transfer API wraps the `Container` automatically; other mods see only the
  buffer and are subject to the filter rule.
- All logic runs server-side; standard menu sync for multiplayer.

## Art direction (mod-wide)

Style: **Vanilla+**, looks like it belongs with vanilla redstone blocks.

- Base materials: smooth stone / iron, vanilla palette, 16×16, top-left lighting.
- Face language shared by all Easy Storage blocks: blue = input, orange = output,
  front face carries the block's function.
- Active state: subtle lit pixel or redstone accent.
- All textures are drawn by us; Mojang textures are not copied into the repo.

Sorter Hopper specifics:
- Vanilla hopper model shape with our own texture and a distinct tint
  (copper or blue, chosen when drawn).
- v1 ships programmatic placeholders for the block texture and screen background;
  final art is a follow-up.

## Testing

Fabric GameTest (`./gradlew runGametest`):

1. Matching item in the container above is pulled and pushed into the container
   in `FACING` direction.
2. Non-matching item stays in the hopper line and passes on to the next hopper.
3. Empty filter pulls nothing.
4. Redstone signal stops transfers.
5. Comparator output ignores the filter slot.
6. Unstackable filter (iron sword) pulls a damaged, enchanted iron sword.
7. Breaking the block drops buffer contents and the filter item.
8. A hopper cannot insert into the filter slot; non-matching pushes are rejected.

Manual check in `runClient`: build an SS3-style line with 3 slices, empty a
mixed chest into it, confirm each item lands in its slice without clogging.

## Out of scope for v1

- Chest Connector (overflow into chests above)
- Exact-match (components) filter toggle
- Configurable speed
- Final textures
- Intake / relay blocks (dropped: vanilla hoppers are used for input)
