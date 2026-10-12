# Overlay Reset on Bank

Temporarily resets movable interface overlays (inventory/side tabs, chat, minimap+orbs) to RuneLite defaults while **banking**, using an **NPC shop**, or optionally other **fixed common interfaces**, then restores the positions RuneLite already saved from your Alt-drag layout.

No manual capture/apply needed.

## Why

Dual-client layouts often drag tabs/chat/minimap toward the center. That can block bank, shop, GE, trade, and similar fixed UIs. Switching RuneLite profiles reloads every plugin and is slow. This plugin only resets those overlays while the interface is open.

## Features

- Reset while banking
- Reset while shopping (NPC shops)
- Optional: **Reset on all fixed common interfaces** (Grand Exchange, deposit box, trade, smithing, collection log, seed vault, world map, reward chests, …)
- Choose which overlays: inventory/tabs, chat, minimap
- Restores your existing RuneLite-saved positions on close
- Crash/close-safe: pre-reset layout is backed up and restored on next start if needed

## Usage

1. Alt-drag your overlays as usual (RuneLite saves them).
2. Enable **Overlay Reset on Bank**.
3. Open bank/shop (or enable the fixed-interfaces toggle) → defaults. Close → your layout returns.

## Development

Requires **JDK 11**.

```bash
./gradlew run
```

Jagex accounts: see [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Plugin Hub submission (next step)

1. Create a **public** GitHub repository (e.g. `overlay-reset-on-bank`) and push this project.
2. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
3. Add `plugins/overlay-reset-on-bank` with:
   ```
   repository=https://github.com/<you>/overlay-reset-on-bank.git
   commit=<full 40-char commit hash>
   ```
4. Open a pull request to plugin-hub and fix any CI / review comments.

## License

BSD 2-Clause
