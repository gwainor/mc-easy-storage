# Easy Storage

Tired of building complex redstone contraptions for your storage when a single block could do the job?

ImpulseSV came up with the [SS3 item filter](https://www.youtube.com/watch?v=NHjWYCDYJ4s) 12 years ago, and Mojang still hasn't added anything to solve storage. Easy Storage solves it with 3 simple blocks: no comparators, no dummy items, no redstone wiring. It works with any item: stackable, 16-stackable (ender pearls, eggs) and unstackable (tools, armor, potions).

## Blocks

### Filter Hopper
A hopper that only accepts the item you put in its filter slot. It's a drop-in replacement for the filter in an SS3 sorter.

- Put one item in the filter slot. Only that item type gets pulled in, whether it stacks to 64, 16 or not at all. Enchantments, durability and names are ignored, so every damaged pickaxe or enchanted book still matches.
- Holds 5 stacks and moves items at normal hopper speed.
- A redstone signal locks it. Comparators read the buffer as they would a normal hopper.
- **Recipe (shapeless):** Hopper + Comparator + Observer

### Multi Filter Hopper
A Filter Hopper with 27 filter slots, so one hopper can sort a whole category: all your ores, all your wood types, all your farm drops.

- Accepts an item if it matches any filter slot.
- Same speed, buffer, redstone lock and comparator behavior as the Filter Hopper.
- **Recipe (shapeless):** Filter Hopper + Chest + Diamond

### Shulker Loader
Packs items into shulker boxes and pushes out each box once it's full. Put one at the end of a sorter line and get boxes ready to carry away.

- Feed it an empty shulker box, then items. When the box is full, it gets dropped out the front like a dropper does (into a container, or as an item).
- A redstone pulse ejects the box early. A steady signal locks it.
- Hoppers never pull items out of the box. A hopper below takes the box itself once it's ready to drop, so you can chain hoppers into stacked chests for extra space.
- Boxes keep their color, name and contents.
- **Recipe (shapeless):** Dropper + Hopper + Shulker Shell

## Requirements
- Minecraft 26.2 or 26.3 (separate downloads)
- Fabric Loader 0.19.5+
- [Fabric API](https://modrinth.com/mod/fabric-api)

Needs to be installed on both client and server.

## Issues & Source
Bug reports and source code: [GitHub](https://github.com/gwainor/mc-easy-storage)

## Development

Requires Java 25 (`brew install openjdk@25`).

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25   # if Java 25 is not your default
./gradlew build        # jar in build/libs/
./gradlew runClient    # dev client
./gradlew runServer    # dev server
./gradlew runGameTest  # in-world GameTests (also run by ./gradlew build)
```

Placeholder textures are generated with `python3 tools/placeholder_textures.py`.
