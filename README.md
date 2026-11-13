# NinjaMinecraft

A small [Paper](https://papermc.io) plugin for Minecraft **26.3**.

- **TPS & MSPT in the tab list.** The header shows the TPS averaged over the last 1, 5 and 15 minutes (labelled 1m / 5m / 15m) and the footer shows
  average and worst MSPT over the last 100 ticks, plus your ping. Values are colour-coded
  (green = healthy, yellow = strained, red = lagging; MSPT over 50 ms means the server can't keep 20 TPS).
- **`/difficulty` without OP.** Every player gets the vanilla `minecraft.command.difficulty` permission.
  Difficulty is server-wide, so anyone can change it for everyone. Turn it off with
  `difficulty.allow-everyone: false`.

## Install

1. Build: `./gradlew build` (needs JDK 25).
2. Copy `build/libs/NinjaMinecraft-<version>.jar` into your server's `plugins/` folder.
3. Restart the server.

## Config

`plugins/NinjaMinecraft/config.yml` controls the refresh interval, the header/footer
([MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, see the placeholders listed in the file),
and the `/difficulty` toggle. Apply changes with `/ninjaminecraft reload` (permission `ninjaminecraft.reload`, OP by default).
