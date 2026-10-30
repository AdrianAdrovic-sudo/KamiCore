# Changelog

## [1.1.0] - 2026-10-30

### Added
- **Kamehameha** (`/kamehameha`, `/kame`): 4 charge levels based on how long you charge. Hold Shift and release to fire. Uses ray tracing to find its target, with blast radius, knockback and a charge bar. Getting hit while charging cancels it.
- **Solar Flare** (`/solarflare`, `/taiyoken`): blinds nearby players who can see you and are facing you. Mobs lose their target.
- **Scouter** (`/scouter [player]`): power levels, health, armor, XP, active Kaioken and coordinates. Scan everyone, or click a name for a full reading.
- **Kaioken levels**: x2, x3, x10 and x20, each stronger and each costing more health when it ends.
- **Gravity levels**: 10x, 50x, 100x and 300x with scaling effects, XP and cooldowns.
- `CHANGELOG.md`

### Changed
- Kaioken and Gravity settings moved to `levels` sections in `config.yml`.
- An existing `config.yml` automatically gets the new settings added on startup.
- New README.

## [1.0.0] - 2026-10-30

### Added
- Ki marks (`/kimark`) and Instant Transmission (`/shunkan`) to marks and coordinates
- Teleport requests (`/kisense`, `/kiaccept`, `/kideny`) with clickable chat buttons
- Flying Nimbus (`/nimbus`), Senzu Bean (`/senzu`), Gravity training (`/gravity`), Kaioken (`/kaioken`)
- Cooldowns that survive restarts, `config.yml`, permissions, GitHub Actions build and release
