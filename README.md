# KamiCore

Dragon Ball inspired abilities for **KamiSMP**, a Paper 26.3 Minecraft server.

## Abilities (v1.0.0)

| Command | Alias | What it does | Default cooldown |
|---|---|---|---|
| `/kimark set\|del\|list [name]` | `/marks` | Save places as ki marks (max 3) | – |
| `/shunkan [mark]` | `/it` | Instant Transmission to one of your ki marks | 30 s |
| `/shunkan [x] [y] [z]` | `/it` | Instant Transmission to coordinates (`~` works) | 30 s |
| `/kisense [player]` | `/tpa` | Ask to teleport to a player | 30 s |
| `/kiaccept [player]` | `/tpaccept` | Accept a request (clickable in chat) | – |
| `/kideny [player]` | `/tpdeny` | Deny a request | – |
| `/nimbus` | – | Toggle the Flying Nimbus (survival flight) | 10 s |
| `/senzu` | – | Full health + hunger | 12 h |
| `/gravity [stop]` | – | 60 s of 100x gravity, then XP | 30 min |
| `/kaioken` | – | Strength II + Speed II for 60 s, then strain | 24 h |
| `/kamicore [reload]` | `/kami` | Help / reload config | – |

All numbers can be changed in `plugins/KamiCore/config.yml`. Cooldowns are saved, so restarts don't reset them.

## Permissions

All abilities are allowed for everyone by default (`kamicore.<command>`).
Ops get `kamicore.admin`, `kamicore.bypass.cooldown` and `kamicore.bypass.marklimit`.

## Building

Requires JDK 25 or newer.

```
gradlew build
```

The plugin jar appears in `build/libs/KamiCore-<version>.jar`.
