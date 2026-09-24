# Easy Storage

Fabric mod for Minecraft 26.2. Adds single blocks that replace complex redstone
contraptions: item sorting, shulker box unloading, and more.

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

### Item Feeder

A barrel-like block that feeds a water-stream sorter. While it holds items it
drops one stack every 8 ticks out of its output face (the orange-ringed side),
no redstone clock needed.

- 27 slots, chest-style screen. Hoppers can feed it from any side.
- Shulker boxes are unloaded stack by stack, then the empty box drops too.
- Place it looking at the spot where items should come out (like a dropper).
- A redstone signal pauses it; it waits while the output face is blocked.
- Recipe (shapeless): barrel + dropper + hopper + repeater.

## Tests

```sh
./gradlew runGameTest   # in-world GameTests (also run by ./gradlew build)
```

Placeholder textures are generated with `python3 tools/placeholder_textures.py`.
