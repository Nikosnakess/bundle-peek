# Bundle Peek

*Hover a bundle, keep its contents open, and grab what you want. A client-side Fabric mod for Minecraft 1.21.11.*

In vanilla Minecraft the contents of a bundle appear in a tooltip that follows your mouse, and you scroll the mouse wheel to choose an item. **Bundle Peek** opens the original tooltip in a fixed window next to the bundle slot. It stays open while your mouse is on the bundle or inside the window, and you can click the items directly.

## Controls

| Action | Result |
|---|---|
| Hover a bundle | Opens the contents window |
| Move the mouse over an item | Selects (highlights) it |
| Left click an item | Takes the whole stack onto the cursor |
| Right click an item | Splits the stack: half on the cursor, the rest stays in the bundle |
| Shift + left click an item | Moves the stack to the other inventory |

It uses the original tooltip, so it keeps the look added by other mods such as [Metal Bundles](https://modrinth.com/mod/metal-bundles). The window does not open while an item is held on the cursor, and it is disabled in the creative inventory.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.1 or newer
- Fabric API

Client-side only: it is not needed on the server.

## Reporting a problem

Please [open an issue](../../issues/new/choose) and include:

- Minecraft, Fabric Loader and Fabric API versions
- The list of installed mods (especially bundle, tooltip and inventory mods such as Metal Bundles or Mouse Tweaks)
- What you did, what you expected and what happened
- The log file `logs/latest.log`, or the crash report if the game crashed

## Building from source

You need JDK 21.

```
./gradlew build          (Windows: gradlew.bat build)
```

The jar is created in `build/libs/` (use the one without `-sources`).

The window placement, item matching and temporary-slot choice are plain Java without Minecraft dependencies and are tested with:

```
javac -d out src/main/java/com/bundlepeek/PanelGeometry.java src/main/java/com/bundlepeek/CellMapper.java src/main/java/com/bundlepeek/SlotChooser.java tests/BundlePeekLogicTest.java
java -cp out BundlePeekLogicTest
```

## Development

Bundle Peek was designed, tested and published by Nikosnakes. The idea, the behaviour, the design choices, the in-game testing and the bug reports are mine. The code and the documentation text were written with the assistance of AI (Claude, by Anthropic), following my instructions.

## License

All Rights Reserved. See the `LICENSE` file.

## Credits

Bundle Peek is an independent mod and is not affiliated with Metal Bundles or Item Interactions (both by Fuzs). It only cooperates with them when they are installed.
