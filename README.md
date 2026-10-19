# Easy Storage

Fabric mod for Minecraft 26.2. Adds single blocks that replace complex redstone
contraptions: item sorting, shulker box loading and more.

## Requirements

- Java 25 (`brew install openjdk@25`)
- Minecraft 26.2, Fabric Loader 0.19.5+, Fabric API

## Build and run

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@25   # if Java 25 is not your default
./gradlew build        # jar in build/libs/
./gradlew runClient    # dev client
./gradlew runServer    # dev server
```

## Blocks

### Filter Hopper

A hopper that only accepts items matching its filter. Drop-in replacement for the
filter hopper in an SS3 item sorter, without comparators or dummy items.

- Put one item in the filter slot (left). Only that item type is pulled in
  (enchantments, damage and names are ignored).
- Holds up to 5 stacks. Pushes like a hopper, at hopper speed.
- A redstone signal locks it. Comparators read only the 5 buffer slots.
- Recipe (shapeless): hopper + comparator + observer.

### Shulker Loader

A dropper-like block that packs items into a shulker box and drops the box when
it is full. Put it under a Filter Hopper at the end of a sorter line.

- Hoppers or droppers feed it one empty shulker box, then items. It holds one box
  at a time; extra boxes are refused until the current one leaves.
- A full box is dropped out of the orange front face like a dropper (into a
  container there, or as an item). A redstone signal locks it.
- A redstone pulse drops the box right away, even if it is not full; a steady
  signal only locks it.
- The box keeps its contents, colour and name. The GUI shows the box slot and its
  27 content slots.
- Recipe (shapeless): dropper + hopper + shulker shell.

## Tests

```sh
./gradlew runGameTest   # in-world GameTests (also run by ./gradlew build)
```

Placeholder textures are generated with `python3 tools/placeholder_textures.py`.
