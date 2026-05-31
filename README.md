# cp_waterfight

Premium **Water Fight** minigame plugin for [Paper](https://papermc.io/) servers — a GunGame-style free-for-all where players level up by eliminating opponents and race to complete the final level.

| | |
|---|---|
| **Version** | 1.0.0 |
| **Target** | Paper 1.21.x |
| **Java** | 21 |

Built for **WorldOfCommunity** as an independent, production-grade minigame plugin.

## Main features

- **Arena setup** — create, configure, validate, and debug arenas via commands
- **Join block & TextDisplay** — physical join blocks with floating status text
- **Match flow** — waiting lobby, countdown, in-game, and ending phases
- **20-level progression** — configurable weapons and kills per level in `levels.yml`
- **Kill tracking** — level-up, win detection, and ranked sidebar scoreboard
- **Protection** — inventory, block, command, and durability safeguards
- **Spectator respawn** — brief post-death phase before re-equip
- **Feedback** — configurable sounds, titles, and actionbar messages
- **Operations** — empty-arena auto-reset, death drop cleanup, throttled protection messages

## Build

Requirements: **JDK 21**. Maven is provided by the project wrapper — no global Maven install needed.

**Windows:**

```bat
cd cp_waterfight
.\mvnw.cmd clean package
```

**Linux / macOS:**

```bash
cd cp_waterfight
./mvnw clean package
```

### Output JAR

```
target/cp_waterfight-1.0.0.jar
```

Copy the JAR into your server's `plugins/` folder and restart. Use `/wf reload` to reload configuration when no matches are active.

## Setup order

1. `/wf create <arena>`
2. `/wf setlobby <arena>`
3. `/wf setjoin <arena>`
4. `/wf addspawn <arena>` (repeat for more spawns)
5. `/wf setmin <arena> <amount>`
6. `/wf setmax <arena> <amount>`
7. `/wf validate <arena>`
8. `/wf debug <arena>` — optional live state check before going live

## Configuration

On first run, the plugin creates these files in `plugins/cp_waterfight/`:

| File | Purpose |
|------|---------|
| `config.yml` | Game flow, protection, scoreboard, spectator, feedback |
| `arenas.yml` | Arena definitions |
| `levels.yml` | Level and weapon progression (20 levels) |
| `messages.yml` | Player-facing messages (legacy `&` colors) |

Startup and `/wf reload` run a **configuration sanity check** — warnings are logged to the console; the plugin stays enabled unless initialization fails fatally.

See [CHANGELOG.md](CHANGELOG.md) and [RELEASE_NOTES.md](RELEASE_NOTES.md) for v1.0.0 details.

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/wf help` | `cpwaterfight.use` | Show available commands |
| `/wf version` | `cpwaterfight.use` | Plugin and stack info |
| `/wf join <arena>` | `cpwaterfight.join` | Join an arena (command or join block) |
| `/wf leave` | `cpwaterfight.join` | Leave your current arena |
| `/wf reload` | `cpwaterfight.admin` | Reload all configuration files |
| `/wf create <arena>` | `cpwaterfight.admin` | Create a new arena |
| `/wf delete <arena>` | `cpwaterfight.admin` | Delete an arena |
| `/wf setlobby <arena>` | `cpwaterfight.admin` | Set lobby to your location (player) |
| `/wf setjoin <arena>` | `cpwaterfight.admin` | Set join block to targeted block (player, 6 blocks) |
| `/wf addspawn <arena>` | `cpwaterfight.admin` | Add spawn at your location (player) |
| `/wf info <arena>` | `cpwaterfight.admin` | Show arena details |
| `/wf list` | `cpwaterfight.admin` | List all arena ids |
| `/wf tp <arena>` | `cpwaterfight.admin` | Teleport to arena lobby (player) |
| `/wf setmin <arena> <n>` | `cpwaterfight.admin` | Set minimum players |
| `/wf setmax <arena> <n>` | `cpwaterfight.admin` | Set maximum players |
| `/wf rename <arena> <name>` | `cpwaterfight.admin` | Set display name |
| `/wf validate <arena>` | `cpwaterfight.admin` | Setup readiness report |
| `/wf debug <arena>` | `cpwaterfight.admin` | Live diagnostics (read-only) |
| `/wf forcestart <arena>` | `cpwaterfight.admin` | Force-start a match |
| `/wf stop <arena>` | `cpwaterfight.admin` | Stop countdown or match |
| `/waterfight` | — | Alias for `/wf` |

Running `/wf` or `/waterfight` without arguments shows help.

## Test checklist

After setup, verify on a test server:

- [ ] Join block and `/wf join` add players to the arena
- [ ] TextDisplay above join block updates player count and state
- [ ] Countdown starts when enough players are in the lobby
- [ ] `/wf forcestart <arena>` starts a match with level 1 kits
- [ ] Kills advance level and re-equip weapons
- [ ] First player to finish the final level wins; match ends cleanly
- [ ] Sidebar scoreboard updates during queue and match
- [ ] Spectator phase after death, then respawn with current weapon
- [ ] Sounds, titles, and actionbar feedback on key events
- [ ] Protections block drops, block break, and griefing while joined
- [ ] `/wf leave` clears scoreboard (and inventory if configured)
- [ ] Empty arena resets when the last player leaves
- [ ] `/wf version` reports **1.0.0**

## Level progression (`levels.yml`)

Water Fight uses GunGame-style progression: **20 levels**, **2 kills per level** (40 kills total to win). The game mode name is *Water Fight* only — weapons are normal, fair combat gear (not water-themed).

- `settings.max-level` — highest level (default `20`)
- `settings.default-kills-required` — kills to advance when a level omits its own value (default `2`)
- `levels.<n>.weapon` — primary item: `material`, `name`, `enchantments`, `extra-items`, etc.

## Scoreboard & feedback

- **Scoreboard** (`config.yml` → `scoreboard`) — sidebar with map, rank, level, weapon, kills, and top 3
- **Feedback** (`config.yml` → `feedback`) — sounds, titles, and actionbar for join, countdown, kills, level-up, and win

Both use legacy `&` colors and `%placeholder%` tokens. See config comments for available placeholders.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `cpwaterfight.use` | `true` | Use `/wf help` and `/wf version` |
| `cpwaterfight.join` | `true` | Join and leave Water Fight arenas |
| `cpwaterfight.admin` | `op` | Arena setup, reload, match control, validate, debug |

## Project layout

```
cp_waterfight/
├── CHANGELOG.md
├── RELEASE_NOTES.md
├── mvnw / mvnw.cmd
├── pom.xml
├── README.md
└── src/main/
    ├── java/de/codingplugs/cpwaterfight/
    └── resources/
        ├── plugin.yml
        ├── config.yml
        ├── arenas.yml
        ├── levels.yml
        └── messages.yml
```

## License

Proprietary — CodingPlugs / WorldOfCommunity. All rights reserved unless otherwise agreed.
