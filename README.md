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

The public game mode name is **Water Fight**. Players join an arena, receive weapons based on their current level, advance by scoring kills, and the first player to finish the final level wins. Full gameplay systems are added in later development steps.

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

The compiled plugin JAR is written to:

```
target/cp_waterfight-1.0.0-SNAPSHOT.jar
```

Copy the JAR into your server's `plugins/` folder and restart (or use a plugin manager reload after the foundation is complete).

## Configuration

On first run, the plugin creates these files in `plugins/cp_waterfight/`:

| File | Purpose |
|------|---------|
| `config.yml` | General plugin settings |
| `arenas.yml` | Arena definitions (future) |
| `levels.yml` | GunGame level and weapon progression (20 levels) |
| `messages.yml` | Player-facing messages (MiniMessage format) |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/wf help` | `cpwaterfight.use` | Show available commands |
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
| `/waterfight` | — | Alias root for `/wf` |

Running `/wf` or `/waterfight` without arguments shows help.

## Level and weapon progression (`levels.yml`)

Water Fight uses a GunGame-style progression: **20 levels**, **2 kills per level** (40 kills total to win when gameplay is complete). The game mode name is *Water Fight* only — weapons are normal, fair combat gear (not water-themed).

`levels.yml` structure:

- `settings.max-level` — highest level (default `20`)
- `settings.default-kills-required` — kills to advance when a level omits its own value (default `2`)
- `levels.<n>.kills-required` — kills needed to reach the next level
- `levels.<n>.weapon` — primary item: `material`, `name`, `amount`, `unbreakable`, `lore`, `enchantments`, `extra-items`

Lore and names support legacy `&` colors and placeholders: `%level%`, `%kills_required%`, `%weapon%`.

`LevelManager` loads definitions and can build `ItemStack` kits from config. Invalid materials or enchantments are logged and skipped safely.

When a match starts, every player begins at **level 1** with the level 1 weapon kit from `levels.yml` (inventory cleared, basic vitals reset, main weapon in hotbar slot 0).

During a match, valid PvP kills in the same arena advance progression (default **2 kills per level**, **20 levels**, **40 kills** total to win). Reaching the final level requirement triggers a winner broadcast and a short ending phase before players return to the lobby. Scoreboard UI follows in a later step.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `cpwaterfight.use` | `true` | Use Water Fight commands |
| `cpwaterfight.join` | `true` | Join a Water Fight arena |
| `cpwaterfight.admin` | `op` | Administrative commands |

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
    │   ├── level/          # LevelDefinition, WeaponDefinition, LevelManager
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
