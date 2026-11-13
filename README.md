# NinjaMinecraft

A small [Paper](https://papermc.io) plugin for Minecraft **26.3**.

- **TPS & MSPT in the tab list.** The header shows the TPS averaged over the last 1, 5 and 15 minutes (labelled 1m / 5m / 15m)
  and the footer shows average and worst MSPT over the last 100 ticks, plus your ping. Values are colour-coded
  (green = healthy, yellow = strained, red = lagging; MSPT over 50 ms means the server can't keep 20 TPS).
- **Stats scoreboard.** A sidebar with the day, in-game time (with a sun/moon icon), weather (with an icon), players online,
  your ping, and TPS and MSPT with load bars.
  Every line is configurable.
- **`/subscribe` (alias `/sub`).** Each player can turn the tab list and scoreboard on or off for themselves:
  `/subscribe` shows your settings, `/subscribe <tablist|scoreboard>` toggles one, and adding `on` / `off` sets it.
  Choices are saved on the player and survive restarts. The default for players who haven't chosen is set per
  display with `default-subscribed`.
- **Sleep messages on top of vanilla sleeping.** The night skip is left to vanilla (via the
  `players_sleeping_percentage` gamerule), so it behaves exactly like vanilla: sleepers must be fully asleep,
  everyone is woken, rain is cleared and phantom timers reset. The plugin announces who is sleeping with a
  count (`(1/2)`), when the night is about to be skipped, when it was skipped and by whom, and when a skip is
  called off because someone got up. OPs can change the required percentage and turn messages on or off with
  `/ninjaminecraft sleep` (permission `ninjaminecraft.sleep.admin`); message text is in `config.yml`.
- **`/difficulty` without OP.** Every player gets the vanilla `minecraft.command.difficulty` permission.
  Difficulty is server-wide, so anyone can change it for everyone. Turn it off with
  `difficulty.allow-everyone: false`.

## Install

1. Build: `./gradlew build` (needs JDK 25).
2. Copy `build/libs/NinjaMinecraft-<version>.jar` into your server's `plugins/` folder.
3. Restart the server.

## Config

`plugins/NinjaMinecraft/config.yml` controls the refresh interval, the tab list header/footer, the scoreboard title and lines
([MiniMessage](https://docs.advntr.dev/minimessage/format.html) format, see the placeholders listed in the file),
the subscription defaults, and the `/difficulty` toggle. Apply changes with `/ninjaminecraft reload` (permission `ninjaminecraft.reload`, OP by default).
