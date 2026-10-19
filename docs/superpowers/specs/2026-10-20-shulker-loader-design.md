# Shulker Loader — Design

- **Date:** 2026-10-20
- **Mod:** Easy Storage (`easystorage`), Fabric, Minecraft 26.2, Java 25
- **Status:** Agreed in brainstorming, builder-checked, pending user review

## Goal

Replace vanilla shulker-box loader contraptions with a single dropper-like block,
the **Shulker Loader**. Hoppers or droppers feed it one empty shulker box and a
stream of items. It fills the box and, when the box is full, drops it out of its
front face like a dropper. Typical use: under a Filter Hopper at the end of a
sorter line, so each sorted item type comes out as packed shulker boxes with no
redstone.

## Behavior

### Placement
- Dropper-like full block with `FACING` in all 6 directions, set like a vanilla
  dropper (faces away from the player; up/down when looking steeply).
- The front (output) face is marked orange, following the mod-wide convention.

### Inventory model
- **Box slot:** holds at most one shulker box (any colour).
- **Content slots:** 27 slots that are the loaded box's contents.
- With no box loaded, the content slots are empty and accept nothing.
- When the box leaves the Loader (drop, player takes it, block broken), it carries
  its contents in its `CONTAINER` data component. Its colour, custom name and
  other components are kept.
- When a box with contents is loaded (by hopper or by hand), its existing contents
  appear in the content slots and filling continues from there.

### Input from automation (hoppers, droppers, any side)
- **A shulker box** goes into the box slot only if the box slot is empty. While a
  box is loaded, every further shulker box is refused from every side until the
  current box leaves.
- **Any other item** goes into the content slots, only while a box is loaded.
  With no box, items are refused (the feeding hopper simply waits).
- Shulker boxes never go into the content slots (vanilla: shulker boxes cannot
  hold shulker boxes).
- If an incoming item does not fit (no empty slot and no part-stack of the same
  item with room), it is refused and the box counts as full (see Output). The
  refused item waits in the feeding hopper and goes into the next box.
- An incoming shulker box while a box is loaded is refused but does **not** make
  the box count as full.

### Output
- **Full box:** the box is full when every content slot holds a full stack, or
  when an incoming non-box item does not fit. A full box is dropped out of the
  front face, unless the Loader is powered (see Redstone). This prevents jams
  from odd part-stacks already in the box.
- **Drop like a dropper:** if there is a container in front, the box is inserted
  into it; if that container cannot take it, the box stays and the Loader retries
  later. If there is no container in front, the box is ejected as an item entity
  the way a dropper ejects items.
- After the box leaves, the Loader is empty and accepts the next box.

### Hopper below
- A hopper under the Loader pulls the items inside the box (content slots),
  one by one like from any container. It never pulls the box itself.

### Redstone
- **Rising edge** (signal turns on): drop the box immediately, full or not. With
  no box loaded, nothing happens.
- **While powered:** the Loader refuses everything from automation (no box, no
  items). The feed line backs up until the signal turns off.
- A constant signal does not keep firing; it only locks.

### Screen
- Opened by right-clicking the block.
- One separate box slot (like the Filter Hopper's filter slot), plus a 3×9 grid of
  the content slots, plus the player inventory.
- The player can put a box in or take the box out by hand. Taking it out takes
  the contents with it.
- The content grid shows the box contents live and lets the player take or add
  items before the box is full. With no box, the grid is locked and drawn
  greyed out.

## Builder's calls (not discussed with the user)
- **Blocked drop is remembered:** a drop (full box or redstone pulse) that cannot
  happen because the container in front is full stays pending, saved with the
  block, and is retried until the box leaves. While a drop is pending the Loader
  accepts no items or boxes from automation.
- **Redstone lock and the hopper below:** the lock blocks input only; a hopper
  below keeps pulling items out while powered.
- **Box emptied by the hopper below:** stays in the Loader until a redstone pulse
  or the player takes it. No eject-when-empty rule (the Loader is a loader, not
  an unloader).
- **Name:** "Shulker Loader", id `easystorage:shulker_loader`.
- **Player use while powered:** the redstone lock blocks automation only; the
  player can still use the screen.
- **Auto-drop triggered by the player:** if the player fills the box to full
  through the screen while unpowered, it drops like any full box.
- **Breaking the block:** drops the Loader plus the box with its contents inside
  (contents are not spilled).
- **Comparator:** reads the content slots like a container (0 with no box).
- **Sound/particles on drop:** same as a vanilla dropper.
- **Recipe (shapeless):** dropper + hopper + shulker shell. Unlocks with a recipe
  advancement like the Filter Hopper.
- **Mining:** pickaxe, like a dropper; loot table drops itself.
- **Creative tab:** Redstone Blocks, after the Filter Hopper.
- **Textures:** generated placeholders via `tools/placeholder_textures.py`.

## Non-goals
- No filtering inside the Loader (the Filter Hopper does that).
- No automatic supply of empty boxes beyond normal hopper/dropper input.
- No sorting of boxes by colour, no config options.

## Testing
- GameTests in `src/gametest`, alongside `FilterHopperGameTest`, covering: box
  acceptance and the one-box rule, item refusal without a box, drop when an
  incoming item does not fit, pending drop when the front is blocked, fill-then-drop into
  a container and as an item entity, redstone pulse drop and lock, hopper-below
  extraction, contents preserved on drop and on block break, loot table.
