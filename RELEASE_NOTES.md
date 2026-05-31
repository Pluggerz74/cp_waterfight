# Water Fight v1.0.0 — Release Notes

**cp_waterfight** is a self-contained Water Fight minigame for Paper servers. Players join an arena, fight through 20 weapon levels, and the first to complete the final level wins.

## Requirements

| Component | Version |
|-----------|---------|
| Server | [Paper](https://papermc.io/) **1.21.x** |
| Java | **21** |

## Installation

1. Download `cp_waterfight-1.0.0.jar` from the release assets.
2. Place the JAR in your server's `plugins/` folder.
3. Start or restart the server.
4. Configure arenas (see setup below).
5. Run `/wf reload` after editing config files, or restart when adding arenas for the first time.

On first run, the plugin creates `plugins/cp_waterfight/` with `config.yml`, `arenas.yml`, `levels.yml`, and `messages.yml`.

## Recommended setup

Run these commands in order for each new arena (requires `cpwaterfight.admin`):

```
/wf create <arena>
/wf setlobby <arena>
/wf setjoin <arena>
/wf addspawn <arena>
/wf setmin <arena> 2
/wf setmax <arena> 16
/wf validate <arena>
/wf debug <arena>
```

Players join with `/wf join <arena>` or by interacting with the join block. Leave with `/wf leave`.

## Known notes

- The public game mode name is **Water Fight**; weapons are normal combat gear configured in `levels.yml`.
- `/wf reload` reloads configuration and resets active match state — use on a test server or when no matches are running.
- Countdown sounds and titles follow the same schedule as chat countdown messages (last 5 seconds and milestone seconds).
- Actionbar feedback runs alongside the sidebar scoreboard; both can be toggled independently in `config.yml`.
- Startup and reload log configuration warnings to the console without disabling the plugin.

## Test checklist

Before going live, verify on a test server:

- [ ] Join block and `/wf join <arena>` add players to the arena
- [ ] TextDisplay above the join block shows player count and state
- [ ] Countdown starts when minimum players are reached
- [ ] `/wf forcestart <arena>` starts a match and equips level 1 weapons
- [ ] Valid PvP kills advance level and re-equip weapons
- [ ] Completing the final level triggers win flow and lobby return
- [ ] Sidebar scoreboard updates during queue and match
- [ ] Spectator phase after death, then respawn with current weapon
- [ ] Sounds, titles, and actionbar feedback fire on key events
- [ ] Protections block drops, inventory abuse, and block griefing
- [ ] `/wf leave` removes scoreboard and clears inventory (if configured)
- [ ] Empty arena resets automatically when the last player leaves
- [ ] `/wf version` reports **1.0.0**
