# cp_waterfight

Premium **Water Fight** minigame plugin for [Paper](https://papermc.io/) servers — a GunGame-style free-for-all where players level up by eliminating opponents and race to complete the final level.

Built for **WorldOfCommunity** as an independent, production-grade minigame plugin.

## Target stack

| Component | Version |
|-----------|---------|
| Minecraft (Paper) | 1.21.x |
| Java | 21 |
| Build | Maven |

## Game mode

The public game mode name is **Water Fight**. Players join an arena, receive weapons based on their current level, advance by scoring kills, and the first player to finish the final level wins.

## Build

Requirements: **JDK 21** only. Maven is provided by the project wrapper (3.9.9) — no global Maven install needed.

**Windows:**

```bat
cd cp_waterfight
mvnw.cmd clean package
```

**Linux / macOS:**

```bash
cd cp_waterfight
./mvnw clean package
```

If you have Maven installed globally, `mvn clean package` works as well.

### Release JAR

The build writes a versioned artifact to:

```
cp_waterfight/target/cp_waterfight-1.0.0-SNAPSHOT.jar
```

Pattern: `cp_waterfight-<version>.jar` (see `version` in `pom.xml`).

Copy the JAR into your server's `plugins/` folder and restart (or reload configuration with `/wf reload` when appropriate).

## Configuration

On first run, the plugin creates these files in `plugins/cp_waterfight/`:

| File | Purpose |
|------|---------|
| `config.yml` | General plugin settings |
| `arenas.yml` | Arena definitions |
| `levels.yml` | GunGame level and weapon progression (20 levels) |
| `messages.yml` | Player-facing messages (legacy `&` colors) |

Startup and `/wf reload` run a **configuration sanity check** — warnings are logged to the console; the plugin stays enabled unless initialization fails fatally.

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
| `/waterfight` | — | Alias root for `/wf` |

Running `/wf` or `/waterfight` without arguments shows help.

### Recommended arena setup order

1. `/wf create <arena>`
2. `/wf setlobby <arena>`
3. `/wf setjoin <arena>`
4. `/wf addspawn <arena>` (repeat for more spawns)
5. `/wf setmin <arena> <amount>`
6. `/wf setmax <arena> <amount>`
7. `/wf validate <arena>`
8. `/wf debug <arena>` — optional live state check before going live

### Quick test checklist

After setup, verify on a test server:

- [ ] Join block opens the arena (`/wf join` or right-click join block)
- [ ] TextDisplay above join block updates player count and state
- [ ] Countdown starts when enough players are in the lobby
- [ ] `/wf forcestart <arena>` starts a match with kits
- [ ] Kills advance level and re-equip weapons
- [ ] First player to finish the final level wins and the match ends cleanly
- [ ] Sidebar scoreboard updates during the match
- [ ] `/wf leave` clears scoreboard (and inventory if configured)
- [ ] Protections block drops, block break, and griefing while joined

## Level and weapon progression (`levels.yml`)

Water Fight uses a GunGame-style progression: **20 levels**, **2 kills per level** (40 kills total to win). The game mode name is *Water Fight* only — weapons are normal, fair combat gear (not water-themed).

`levels.yml` structure:

- `settings.max-level` — highest level (default `20`)
- `settings.default-kills-required` — kills to advance when a level omits its own value (default `2`)
- `levels.<n>.kills-required` — kills needed to reach the next level
- `levels.<n>.weapon` — primary item: `material`, `name`, `amount`, `unbreakable`, `lore`, `enchantments`, `extra-items`

Lore and names support legacy `&` colors and placeholders: `%level%`, `%kills_required%`, `%weapon%`.

When a match starts, every player begins at **level 1** with the level 1 weapon kit from `levels.yml` (inventory cleared, basic vitals reset, main weapon in hotbar slot 0).

During a match, valid PvP kills in the same arena advance progression. Reaching the final level requirement triggers a winner broadcast and a short ending phase before players return to the lobby.

## Sidebar scoreboard (`config.yml` → `scoreboard`)

While a player is in an arena queue or match, a per-player sidebar can show map, rank, level, weapon, kill progress, and the top 3 players. Toggle with `scoreboard.enabled` and set `update-interval-ticks` (default `20`).

Configurable `scoreboard.title` and `scoreboard.lines` use legacy `&` colors and placeholders (`%map%`, `%rank%`, `%level%`, `%weapon%`, `%kills%`, top player slots, etc.).

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `cpwaterfight.use` | `true` | Use `/wf help`, `/wf version`, and base command access |
| `cpwaterfight.join` | `true` | Join and leave Water Fight arenas |
| `cpwaterfight.admin` | `op` | Setup, reload, match control, validate, debug |

## Project layout

```
cp_waterfight/
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
├── .mvn/wrapper/
│   └── maven-wrapper.properties
└── src/main/
    ├── java/de/codingplugs/cpwaterfight/
    │   ├── CPWaterFight.java
    │   ├── config/         # ConfigManager, ConfigSanityChecker
    │   ├── diagnostics/    # ArenaDebugReporter
    │   ├── level/
    │   ├── scoreboard/
    │   ├── game/
    │   ├── arena/
    │   └── ...
    └── resources/
        ├── plugin.yml
        ├── config.yml
        ├── arenas.yml
        ├── levels.yml
        └── messages.yml
```

## License

Proprietary — CodingPlugs / WorldOfCommunity. All rights reserved unless otherwise agreed.
