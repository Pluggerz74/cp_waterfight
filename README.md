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
| `levels.yml` | Level and weapon progression (future) |
| `messages.yml` | Player-facing messages (MiniMessage format) |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/wf help` | `cpwaterfight.use` | Show available commands |
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
    │   └── CPWaterFight.java
    └── resources/
        ├── plugin.yml
        ├── config.yml
        ├── arenas.yml
        ├── levels.yml
        └── messages.yml
```

## License

Proprietary — CodingPlugs / WorldOfCommunity. All rights reserved unless otherwise agreed.
