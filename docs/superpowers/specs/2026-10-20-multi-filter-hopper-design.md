# Multi Filter Hopper — Design

- **Date:** 2026-10-20
- **Mod:** Easy Storage (`easystorage`), Fabric, Minecraft 26.2, Java 25
- **Status:** Agreed in chat; user asked to skip further questions and build

## Goal

A second, pricier filter block: the **Multi Filter Hopper**. It is the Filter
Hopper with 27 filter slots instead of one. It accepts any item whose type matches
any filter slot. Used in storage systems and farms; players group items however
they like (2 items, a category, any mix). The block imposes no categories.

Kept as a separate block (not an upgrade of the Filter Hopper) so that sorting
several items per slice costs more in game. Code is shared underneath so the two
blocks cannot drift apart.

## Behavior (same as the Filter Hopper unless stated)

- Hopper shape, placement (`FACING` down or horizontal), `ENABLED` redstone lock,
  hopper transfer speed, 5-slot buffer, pulls from above / item entities, pushes
  out of its spout.
- **Filter:** 27 separate slots, each holding one real item (max stack 1). Only the
  player changes them through the screen; automation never sees them.
- **Matching:** an item is accepted when its type equals the type of any non-empty
  filter slot (components ignored, as in the Filter Hopper). Empty slots are
  ignored. All 27 empty → accepts nothing. Duplicate filter entries are harmless.
- Changing the filter does not touch items already in the buffer.
- Comparators read only the 5 buffer slots.
- Breaking drops the block, the buffer and all filter items.

### Screen
- 3×9 filter grid (tinted like the Filter Hopper's filter slot), the 5 buffer
  slots below it, then the player inventory.
- **Shift-click from the player inventory:** if the item matches the filter, it
  goes into the buffer; otherwise one of it goes into the first empty filter slot;
  if the filter is full and it doesn't match, nothing moves. (For the Filter Hopper
  this is exactly its current behavior, so both blocks share one rule.)
- Shift-click from a filter or buffer slot moves it to the player inventory.

## Builder's calls
- **Name:** "Multi Filter Hopper", id `easystorage:multi_filter_hopper` (user agreed).
- **Recipe (shapeless):** Filter Hopper + chest + diamond (user agreed). Unlocks
  when the player gets a Filter Hopper.
- **Code sharing:** the Multi Filter Hopper's block, block entity, menu and screen
  are the Filter Hopper's classes parameterised by filter size (and layout), not
  copies.
- **Save format:** filter saved as a slot list. The Filter Hopper's old single
  `filter` key is still read into slot 0 so existing dev worlds keep their filter.
- **Textures:** generated placeholders in a different tint (purple) via
  `tools/placeholder_textures.py`; models reuse vanilla hopper parents like the
  Filter Hopper's.
- **Mining:** pickaxe; loot table drops itself (custom name copied).
- **Creative tab:** Redstone Blocks, after the Filter Hopper.
- **Filter item shown on top of the block:** not in scope (still a TODO for the
  Filter Hopper).

## Non-goals
- No tag/category matching, no exact-component match toggle, no config.
- No changes to Filter Hopper behavior beyond the shared code.

## Testing
- GameTests mirroring `FilterHopperGameTest` for the Multi Filter Hopper: two
  different filter items both get through while others continue along the line,
  empty filter accepts nothing, redstone lock, comparator ignores filters, breaking
  drops buffer + all filters, hopper can't fill filter slots, shift-click rule
  (match → buffer, new item → next filter slot, full filter + no match → stays).
- Existing Filter Hopper tests stay green; plus a test that the legacy single
  `filter` save key loads into slot 0.
- Loot table test for the new block.
