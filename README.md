# NaN Lab — Fabric 26.3

A controlled Minecraft Java Edition experiment mod inspired by the NaN-value demonstrations in **“Breaking Minecraft with the 'NaN' value”**.

## Target

- Minecraft: **26.3**
- Loader: Fabric **0.19.5 or newer**
- Fabric API: **0.165.0+26.3**
- Mod Menu: **21.0.0** (optional, for the in-game control panel)
- Java: **25**

Minecraft 26.x uses Mojang's unobfuscated names. This project therefore does not use Yarn mappings.

## Features

- `/nan set <field> <value>` — applies a value to yourself.
- `/nan set <player> <field> <value>` — applies a value to a selected player.
- `/nan reset [player]` — moves the target to the current dimension's shared spawn and restores safe player values.
- `/nan list` and `/nan help` — show supported fields and syntax.
- Mod Menu integration — opens a button panel for the common NaN experiments.

Accepted values: `nan`, `inf`, `-inf`, or any Java floating-point number such as `0.25` or `-3.5`.

### Individual fields

| Field | Stored type / affected value |
|---|---|
| `x`, `y`, `z` | double position coordinates |
| `motion_x`, `motion_y`, `motion_z` | double velocity components |
| `yaw`, `pitch` | float rotation values |
| `fall_distance` | float fall distance |
| `walk_speed`, `fly_speed` | float player ability speed fields |
| `movement_speed`, `armor`, `attack_damage`, `luck` | double attribute base values |
| `health`, `absorption` | float health / absorption values |
| `saturation`, `exhaustion` | float food values |
| `xp_progress` | float XP-bar progress |

### Groups

`position`, `motion`, `rotation`, `speeds`, `attributes`, `vitals`, `hunger`, and `all`.

Examples:

```text
/nan set x nan
/nan set motion_y nan
/nan set rotation nan
/nan set health nan
/nan set attributes nan
/nan set all nan
/nan set Notch health nan
/nan reset
```

The target-name example is illustrative; use a player name that is actually online.

## Important behavior / safety

- This mod changes the selected player's server-side values through ordinary commands. It does **not** send malformed packets, bypass permissions, or attempt to crash a remote server.
- Commands require cheats in singleplayer or permission level 2 (operator) on a server.
- NaN is kept in floating-point fields as `Float.NaN` / `Double.NaN`. The implementation deliberately does not pass the value through an integer conversion. In Java, converting NaN to an integer produces zero, which would erase the value.
- `xp_progress` is the floating-point XP-bar progress field; integer XP totals and levels cannot store NaN.
- Some values may be changed again by normal game ticks, attributes, or server logic. Results can vary by Minecraft implementation and installed mods.
- **Back up worlds before experimenting.** Applying NaN to position or health can make a player difficult to recover. `/nan reset` is a recovery command, but it cannot guarantee recovery from every possible broken world state.

## Build

Use JDK 25 and Gradle 9.6 or newer. From this folder run:

```bash
gradle build
```

The output JAR will be in `build/libs/`.

Install the built JAR in the Fabric `mods` folder. Install Fabric API too. For the Mod Menu button panel, install Mod Menu 21.0.0 for Minecraft 26.3.

## Implementation note

The speed and XP/fall-distance fields are accessed reflectively because their implementation details are more likely to vary between Minecraft versions. If Mojang changes those field names in a later build, `/nan` reports the specific field that could not be accessed instead of silently converting or substituting a value.
