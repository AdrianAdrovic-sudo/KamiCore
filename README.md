<div align="center">

# ⚡ KamiCore

### Dragon Ball powers for Minecraft

A custom Paper plugin built from scratch for **KamiSMP**, a Dragon Ball inspired survival server.<br>
Transform into a Super Saiyan, blast craters with a Final Flash, hunt the 7 Dragon Balls and make a wish to Shenron.

[![Build](https://github.com/AdrianAdrovic-sudo/KamiCore/actions/workflows/build.yml/badge.svg)](https://github.com/AdrianAdrovic-sudo/KamiCore/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/AdrianAdrovic-sudo/KamiCore?color=orange&label=release)](https://github.com/AdrianAdrovic-sudo/KamiCore/releases/latest)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-62B47A?logo=minecraft&logoColor=white)
![Paper](https://img.shields.io/badge/Paper%20API-26.3-F7CF0D)
![Java](https://img.shields.io/badge/Java-25-E76F00?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

[**Download**](https://github.com/AdrianAdrovic-sudo/KamiCore/releases/latest) · [Features](#-features) · [Commands](#-commands) · [Configuration](#%EF%B8%8F-configuration) · [How it works](#-how-it-works) · [Building](#-building-from-source)

</div>

---

## ✨ Features

### 🟡 Super Saiyan: 7 transformations
Every form drains your **XP as ki** each second. Higher forms give bigger buffs, more hearts and flight, but burn through your XP faster. When it runs out, you power down.

| Form | Command | Glow | XP drain | Buffs | Extra hearts | Special |
|---|---|:---:|:---:|---|:---:|---|
| Super Saiyan | `/ssj ssj` | 🟡 gold | 2/s | Strength I, Speed I | +2 | Ki ×1.2 |
| Super Saiyan 2 | `/ssj ssj2` | 🟡 gold | 3/s | Strength II, Resistance I | +4 | Lightning aura |
| Super Saiyan 3 | `/ssj ssj3` | 🟨 yellow | 5/s | Strength II, Speed II | +6 | Lightning aura |
| Super Saiyan 4 | `/ssj ssj4` | 🟥 crimson | 6/s | Strength III, Regeneration | +8 | Lightning aura |
| Super Saiyan God | `/ssj god` | 🔴 red | 8/s | Strength III, Resistance II | +10 | Ki ×1.85 |
| Super Saiyan Blue | `/ssj blue` | 🔵 aqua | 10/s | Strength IV, Speed III | +12 | Ki ×2.0 |
| Ultra Instinct | `/ssj ui` | ⚪ silver | 15/s | Strength IV, Speed IV, Resistance III | +15 | **30% auto-dodge** |

The "hair color" change is a colored particle aura **plus a colored glow outline and name color** that everyone can see. Transforming strikes lightning.

- **Ki flight:** double-tap jump in the air to fly like an elytra, steering where you look. Higher forms fly faster (up to 2× for Ultra Instinct). Sneak to land; no fall or crash damage while transformed.
- **Powered-up ki attacks:** forms (and Kaioken) make every ki attack stronger, **bigger and longer-ranged**, with beams wrapped in your aura color.

### 💥 Ki attacks that reshape the world
Every attack charges up through 4 levels. Hold **Shift** while charging and release to fire. Every attack destroys terrain.

| Attack | Command | Style | Max level |
|---|---|---|---|
| 🔵 **Kamehameha** | `/kamehameha` | Blue beam | 10 ❤, 50 blocks, crater 4 |
| 🟣 **Galick Gun** | `/galickgun` | Purple beam, charges fastest | 9 ❤, 45 blocks, crater 3.5 |
| 🟡 **Final Flash** | `/finalflash` | Golden beam, slow but devastating | 14 ❤, 70 blocks, crater 6 |
| 🌀 **Special Beam Cannon** | `/specialbeamcannon` | Spiral beam that **pierces** mobs, players and blocks | 11 ❤ to everything in a 70-block line |
| ⚪ **Spirit Bomb** | `/spiritbomb` | Giant flying energy ball; **friends hold Shift to help charge it** | 16 ❤, blast radius 7, crater 8 |

Getting hit while charging breaks your focus. Chests, obsidian and bedrock survive explosions by default.

### 🐉 Dragon Balls & Shenron
- **7 glowing Dragon Balls** are hidden across the world, up to 1,500 blocks from spawn.
- Track them with the **Dragon Radar** (`/scouter dragonballs`): an arrow and the distance to the nearest ball.
- Right-click a ball to collect it. Balls can be **dropped, stored, traded or stolen** when a holder dies.
- Collect all 7 and use **`/shenron`**. The sky darkens, lightning strikes, and the Eternal Dragon opens a wish menu:

| Wish | Reward |
|---|---|
| 🛡️ Legendary Gear | Full netherite armor and sword with max enchantments |
| 💎 Riches | 64 diamonds, 16 netherite ingots, 50 XP levels |
| ❤️ Immortal Body | +10 max hearts for 7 days |
| ⭐ Unlimited Power | No cooldowns on any ability for 1 hour |

After a wish the balls **turn to stone for 24 hours**, then scatter again. Each cycle has a unique ID, so leftover or duplicated balls are worthless. Balls lost in the void fly to a new hiding place.

### 🔴 Kaioken & 🏋️ Gravity Chamber
- **Kaioken x2, x3, x10 and x20**: more Strength and Speed per level, and ki attacks up to 1.8× stronger with a red aura. You lose 15–80% of your max health when it ends.
- **Gravity training at 10x, 50x, 100x or 300x**: heavier effects mean more XP (60 → 800).

### ☀️ Solar Flare · 📡 Scouter
- **Solar Flare**: blinds every nearby player who is looking at you. Walls protect them, and mobs lose track of you.
- **Scouter**: read anyone's power level, health, armor, transformation, active wishes, Dragon Balls held and coordinates. Yes, it says *IT'S OVER 9000!*

### 🌀 Instant Transmission · ☁️ Nimbus · 🫘 Senzu
Ki marks you can teleport to, coordinate teleports, clickable teleport requests, survival flight on the Flying Nimbus, and a full heal from a Senzu Bean.

---

## 🎮 Commands

| Command | Alias | Description |
|---|---|---|
| `/ssj [form\|off]` | `/supersaiyan`, `/transform` | List forms, transform, or power down |
| `/kamehameha` | `/kame` | Charge and fire a Kamehameha |
| `/galickgun` | `/galick` | Charge and fire a Galick Gun |
| `/finalflash` | | Charge and fire a Final Flash |
| `/specialbeamcannon` | `/sbc`, `/makankosappo` | Charge and fire a piercing beam |
| `/spiritbomb` | `/genkidama` | Gather energy for a Spirit Bomb |
| `/scouter [player\|dragonballs]` | | Scan power levels, or track Dragon Balls |
| `/shenron` | | Summon Shenron with all 7 Dragon Balls |
| `/kaioken [x2\|x3\|x10\|x20]` | | Power boost that costs health |
| `/gravity [10x\|50x\|100x\|300x\|stop]` | | Gravity training for XP |
| `/solarflare` | `/taiyoken` | Blind everyone looking at you |
| `/kimark set\|del\|list [name]` | `/marks` | Manage your ki marks |
| `/shunkan [mark]` · `/shunkan [x] [y] [z]` | `/it` | Instant Transmission |
| `/kisense [player]` · `/kiaccept` · `/kideny` | `/tpa` · `/tpaccept` · `/tpdeny` | Teleport requests |
| `/nimbus` | | Toggle the Flying Nimbus |
| `/senzu` | | Full health and hunger |
| `/kamicore [reload]` | `/kami` | Help, or reload the config |
| `/dragonballs status\|scatter\|give` | | Admin: manage the Dragon Balls |

All commands have tab-completion.

## 🔑 Permissions

| Permission | Default | Description |
|---|:---:|---|
| `kamicore.<command>` | everyone | Use that ability (e.g. `kamicore.finalflash`, `kamicore.supersaiyan`) |
| `kamicore.admin` | op | `/kamicore reload` and `/dragonballs` |
| `kamicore.bypass.cooldown` | op | Ignore all cooldowns |
| `kamicore.bypass.marklimit` | op | Unlimited ki marks |

---

## ⚙️ Configuration

Every number lives in `plugins/KamiCore/config.yml` (forms, attack levels, wishes, destruction rules), and `/kamicore reload` applies changes without a restart. Everything is data-driven, so you can add a level or a whole new form without touching code:

```yaml
supersaiyan:
  forms:
    rose:                    # adds /ssj rose
      name: Super Saiyan Rosé
      color: light_purple
      aura: "#FF5FBF"
      xp-drain: 12
      strength: 4
      speed: 3
      extra-hearts: 12
      flight: true
      ki-multiplier: 2.2
```

```yaml
destruction:
  enabled: true                 # false = ki attacks never break blocks
  protect-containers: true      # chests, barrels, shulkers are never destroyed
  max-blast-resistance: 1000    # obsidian, bedrock, ancient debris survive
  disabled-worlds: []
```

Cooldowns, ki marks, timed wishes and Dragon Ball state are all saved to disk and survive restarts.

---

## 🧠 How it works

KamiCore is written in **Java 25** against the **Paper API**, with no dependencies besides Paper itself.

- **Modern command system:** every command implements Paper's Brigadier-backed `BasicCommand`, registered in code with aliases, permissions and tab-completion.
- **Shared ki attack engine:** all 5 attacks run on one charge state machine with three delivery types: ray-traced **beams** (`World#rayTrace`), a **piercing** line that steps along the look vector, and a **projectile** simulated tick by tick. Each attack only defines its look in an enum; its numbers come from config.
- **Vector math:** beam rendering, spiral particles (cross products for perpendicular vectors), knockback vectors, and a direction arrow computed from yaw for the Dragon Radar.
- **Terrain destruction:** randomized spherical craters with protection rules based on blast resistance, container detection and per-world toggles.
- **Ki flight:** elytra-style gliding without an elytra, kept alive by cancelling `EntityToggleGlideEvent` and steered with a per-tick velocity that eases toward the look direction.
- **Super Saiyan:** a single repeating task handles auras and XP drain (`calculateTotalExperiencePoints`). Extra hearts come from namespaced **attribute modifiers**, and the colored glow uses **scoreboard teams**. The player's previous team is restored afterwards, and leftovers are cleaned up after a crash.
- **Dragon Balls:** balls are **ItemDisplay + Interaction entities** tagged with the **Persistent Data Container**. Chunks load asynchronously (`getChunkAtAsync`) so scattering never freezes the server, a **cycle ID** makes duplicated balls worthless, and stale entities are purged on `EntitiesLoadEvent`. Lost items re-scatter themselves.
- **Custom inventory GUI:** Shenron's wish menu is a custom `InventoryHolder`, with all clicks and drags handled safely.
- **Persistence:** YAML storage keyed by player UUID for cooldowns, timed wishes (re-applied on join) and Dragon Ball state.
- **CI/CD:** GitHub Actions builds every push. Pushing a version tag publishes a release with the compiled jar.

### Project structure

```
src/main/java/com/kamismp/kamicore/
├── KamiCore.java                 # Entry point: registers commands, listeners, tasks
├── commands/                     # One class per command (BasicCommand)
├── ki/
│   ├── KiAttacks.java            # Charge state machine, beams, piercing, Spirit Bomb, impacts
│   ├── KiStyle.java              # Look and behavior of each attack
│   └── Destruction.java          # Crater generation with protection rules
├── saiyan/
│   ├── SuperSaiyan.java          # Transformations, XP drain, auras, teams, attribute modifiers
│   └── SaiyanForm.java           # A form loaded from config
├── dragonballs/
│   ├── DragonBalls.java          # Scattering, collecting, cycles, radar data
│   ├── Wishes.java               # Shenron summoning and the 4 wishes
│   ├── WishMenu.java             # Wish GUI
│   └── DragonBallListener.java   # Collecting, item safety, GUI and anti-exploit rules
├── data/DataStore.java           # YAML persistence
├── listeners/PlayerListener.java # Join, quit, death, damage, dodge
└── util/                         # Cooldowns, effects, messages, levels, tab-completion
```

---

## 📦 Installation

1. Download `KamiCore-x.x.x.jar` from the [latest release](https://github.com/AdrianAdrovic-sudo/KamiCore/releases/latest).
2. Put it in your server's `plugins/` folder.
3. Restart the server (Paper **26.3+**, Java **25+**).

## 🔨 Building from source

Requires **JDK 25** or newer.

```bash
git clone https://github.com/AdrianAdrovic-sudo/KamiCore.git
cd KamiCore
./gradlew build        # Windows: .\gradlew build
```

The jar is created in `build/libs/`.

---

## 🗺️ Version history

- [x] **1.0.0:** Instant Transmission, ki marks, teleport requests, Nimbus, Senzu, Gravity, Kaioken
- [x] **1.1.0:** Kamehameha, Solar Flare, Scouter, Kaioken levels, Gravity levels
- [x] **2.0.0:** Super Saiyan forms, Final Flash, Galick Gun, Special Beam Cannon, Spirit Bomb, terrain destruction, Dragon Balls and Shenron
- [x] **2.1.0:** Elytra-style ki flight; forms and Kaioken power up ki attacks

See [CHANGELOG.md](CHANGELOG.md) for details.

---

<div align="center">

Made by **Adrian Adrović**<br>
Built as a hobby project for KamiSMP · [MIT License](LICENSE)

*Dragon Ball is the property of Akira Toriyama, Shueisha and Toei Animation. This is a non-commercial fan project.*

</div>
