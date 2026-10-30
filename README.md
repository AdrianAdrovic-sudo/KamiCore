<div align="center">

# ⚡ KamiCore

### Dragon Ball powers for Minecraft

A custom Paper plugin built from scratch for **KamiSMP**, a Dragon Ball inspired survival server.<br>
Charge a Kamehameha, push your body with Kaioken, train under 300x gravity and Instant Transmission across the map.

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

### 🔵 Kamehameha
The longer you charge, the stronger it gets. Hold **Shift** while charging and release to fire, or type the command again.

| Level | Charge time | Damage | Range | Blast radius |
|:---:|:---:|:---:|:---:|:---:|
| 1 | instant | 2 ❤ | 15 blocks | – |
| 2 | 1.5 s | 4 ❤ | 25 blocks | 1.5 |
| 3 | 3 s | 6.5 ❤ | 35 blocks | 2.5 |
| 4 | 5 s | 10 ❤ | 50 blocks | 4 |

A glowing ki ball grows in your hands while a *KA... ME... HA... ME...* bar fills up. The beam uses ray tracing to find its target, knocks enemies back and explodes on impact (no block damage). Getting hit while charging breaks your focus.

### 🔴 Kaioken
Four levels of power, each with a heavier price paid in health when it ends.

| Level | Strength | Speed | Duration | Health lost after |
|:---:|:---:|:---:|:---:|:---:|
| x2 | I | I | 60 s | 15% |
| x3 | II | II | 45 s | 30% |
| x10 | III | II | 30 s | 55% |
| x20 | IV | III | 20 s | 80% |

A red aura grows with the level. Kaioken never kills you, but it can leave you on half a heart.

### 🏋️ Gravity Chamber
Train under **10x, 50x, 100x or 300x** gravity. Heavier gravity means stronger Slowness, Mining Fatigue and Hunger, and a bigger XP reward (60 → 800 XP). Die or quit and the training is lost.

### ☀️ Solar Flare
A blinding flash. Every player within 10 blocks **who is looking at you** goes blind for 5 seconds. Walls protect them, and mobs lose track of you.

### 📡 Scouter
Read anyone's **power level**, health, armor, XP level, active Kaioken and coordinates. Scan the whole server at once and click a name for a full reading. Scouters still beep *IT'S OVER 9000!*

### 🌀 Instant Transmission
Save up to 3 **ki marks** and teleport to them instantly, jump to exact coordinates (with `~` relative support), or lock onto a friend's ki with a clickable request.

### ☁️ Flying Nimbus · 🫘 Senzu Bean
Fly in survival on Kintoun (get hit and you fall off), or eat a Senzu Bean for a full heal on a 12-hour cooldown.

---

## 🎮 Commands

| Command | Alias | Description | Default cooldown |
|---|---|---|:---:|
| `/kamehameha` | `/kame` | Charge, then release Shift (or retype) to fire | 20 s – 5 min |
| `/kaioken [x2\|x3\|x10\|x20]` | | Power boost that costs health | 12 – 24 h |
| `/gravity [10x\|50x\|100x\|300x]` | | Gravity training for XP · `/gravity stop` to quit | 10 min – 1 h |
| `/solarflare` | `/taiyoken` | Blind every player looking at you | 5 min |
| `/scouter [player]` | | Scan everyone, or get a full reading of one player | 15 s |
| `/kimark set\|del\|list [name]` | `/marks` | Manage your ki marks | – |
| `/shunkan [mark]` | `/it` | Instant Transmission to a ki mark | 30 s |
| `/shunkan [x] [y] [z]` | `/it` | Instant Transmission to coordinates | 30 s |
| `/kisense [player]` | `/tpa` | Ask to teleport to a player | 30 s |
| `/kiaccept` · `/kideny` | `/tpaccept` · `/tpdeny` | Answer a request (or click in chat) | – |
| `/nimbus` | | Toggle the Flying Nimbus | 10 s |
| `/senzu` | | Full health and hunger | 12 h |
| `/kamicore [reload]` | `/kami` | Help, or reload the config | – |

All commands have tab-completion.

## 🔑 Permissions

| Permission | Default | Description |
|---|:---:|---|
| `kamicore.<command>` | everyone | Use that ability (e.g. `kamicore.kamehameha`) |
| `kamicore.admin` | op | `/kamicore reload` |
| `kamicore.bypass.cooldown` | op | Ignore all cooldowns |
| `kamicore.bypass.marklimit` | op | Unlimited ki marks |

---

## ⚙️ Configuration

Every number is editable in `plugins/KamiCore/config.yml`, and `/kamicore reload` applies changes without a restart. Levels are plain YAML, so you can add your own:

```yaml
kaioken:
  levels:
    x50:              # adds /kaioken x50
      strength: 5
      speed: 3
      duration: 10
      health-cost: 0.95
      strain: 90
      cooldown: 172800
```

Cooldowns and ki marks are saved to `data.yml`, so they survive server restarts.

---

## 🧠 How it works

KamiCore is written in **Java 25** against the **Paper API** and has no dependencies besides Paper itself.

- **Modern command system:** every command implements Paper's Brigadier-backed `BasicCommand` interface. Commands are registered in code with aliases, permissions and tab-completion, not through `plugin.yml`.
- **Ray tracing and vector math:** Kamehameha uses `World#rayTrace` along the player's look vector to find the first entity in the path. The beam is drawn by stepping along the direction vector, and knockback is applied as a velocity vector.
- **Tick-based state machines:** charging, Kaioken auras and gravity training run on the Bukkit scheduler (20 ticks = 1 s). They clean up after themselves when a player dies, logs out or the server stops.
- **Persistent cooldowns:** cooldowns are stored as "ready at" timestamps per player UUID in YAML, so long cooldowns can't be reset by a restart.
- **Data-driven design:** ability levels are read from config sections at runtime, so new levels need no code changes. Older configs are upgraded automatically.
- **Rich text UI:** MiniMessage is used for gradients, hover text and clickable chat buttons (teleport requests, scouter results, ki mark lists).
- **CI/CD:** GitHub Actions builds every push. Pushing a version tag automatically publishes a release with the compiled jar.

### Project structure

```
src/main/java/com/kamismp/kamicore/
├── KamiCore.java              # Plugin entry point: registers commands and listeners
├── commands/                  # One class per command (BasicCommand)
│   ├── KamehamehaCommand.java # Charge state machine, ray tracing, beam rendering
│   ├── KaiokenCommand.java    # Levelled buffs + health cost
│   ├── GravityCommand.java    # Levelled training sessions
│   ├── SolarFlareCommand.java # Line-of-sight + facing checks
│   ├── ScouterCommand.java    # Power level formula, player scanning
│   └── ...                    # Instant Transmission, ki marks, Nimbus, Senzu
├── data/DataStore.java        # YAML persistence for marks and cooldowns
├── listeners/PlayerListener.java  # Death, logout, damage, world change
└── util/                      # Cooldowns, effects, messages, level parsing
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

## 🗺️ Roadmap

- [x] **1.0.0:** Instant Transmission, ki marks, teleport requests, Nimbus, Senzu, Gravity, Kaioken
- [x] **1.1.0:** Kamehameha, Solar Flare, Scouter, Kaioken levels, Gravity levels
- [ ] Combat tag (no teleporting out of PvP)
- [ ] Super Saiyan transformation, unlocked through training
- [ ] Dragon Balls: 7 hidden orbs, a Dragon Radar and wishes

See [CHANGELOG.md](CHANGELOG.md) for version history.

---

<div align="center">

Made by **Adrian Adrović**<br>
Built as a hobby project for KamiSMP · [MIT License](LICENSE)

*Dragon Ball is the property of Akira Toriyama, Shueisha and Toei Animation. This is a non-commercial fan project.*

</div>
