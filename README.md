# NaN Lab — Fabric 26.3

NaN Lab is a controlled numerical-value experiment mod for Minecraft Java 26.3.

## Target toolchain

- Minecraft: **26.3**
- Fabric Loader: **0.19.5 or newer**
- Fabric API: **0.165.0+26.3**
- Fabric Loom: **1.19-SNAPSHOT**
- Java: **25**
- Gradle: **9.8.2** (as pinned in the GitHub Actions workflow)
- Mod Menu: **21.0.0** (optional)

Minecraft 26.1+ uses Mojang's unobfuscated names, so this project does not use Yarn mappings.

## Global numeric command arguments (1.3.1)

NaN Lab patches Brigadier's shared `IntegerArgumentType`, `LongArgumentType`, `FloatArgumentType`, and `DoubleArgumentType` parsers. For those standard numeric arguments, the mod reads the number directly and bypasses Brigadier's per-argument min/max validation. It also patches `StringReader.readInt/readLong/readFloat/readDouble` so special tokens work for Minecraft parsers that use those shared readers directly.

This means the parser accepts ordinary negative values such as `-1` even in standard command arguments that normally declare a positive minimum, and accepts `NaN`, `inf`, `-inf`, `Infinity`, and `-Infinity` as appropriate. It applies across commands that use these standard Brigadier number arguments, not only to a hard-coded list of command names. Command execution can still reject, clamp, overflow, or reinterpret values after parsing. This is a broad parser-layer patch, not a guarantee that every command's gameplay logic supports every value.

| Input | Float/double argument | Int/long argument |
|---|---|---|
| `NaN` | actual IEEE NaN | `0` |
| `inf` / `Infinity` | positive infinity | integer maximum |
| `-inf` / `-Infinity` | negative infinity | integer minimum |
| `-1` | ordinary negative number | ordinary negative integer |
| `100000` | ordinary number | ordinary integer |

Examples:

```text
/tp @s NaN NaN NaN
/tp @s inf inf inf
/attribute @s minecraft:generic.movement_speed base set -1
/playsound minecraft:block.note_block.pling master @s ~ ~ ~ inf 1
/scoreboard players set @s test -1
/give @s minecraft:dirt -1
/enchant @s minecraft:knockback NaN
/enchant @s minecraft:sharpness 100000
/enchant @s minecraft:sharpness -1
```

**Important:** Integer and long destinations cannot physically store IEEE NaN or infinity. They receive deterministic JVM-style conversions. For doubles/floats, a command may still have its own post-parse checks or the target API may clamp the value.

**Scope:** this applies to standard Brigadier numeric argument classes and Minecraft parsers that call the patched `StringReader` numeric methods. Therefore many vanilla commands and mod commands benefit automatically without individual command registrations. It does not rewrite booleans, selectors, resource IDs, item predicates, enums, literals, custom JSON/SNBT parsers, or every bespoke mod parser. A custom command may also validate its parsed number after Brigadier returns. Integer and long arguments remain bounded by their Java storage types; float and double arguments use IEEE-754 values.

### `/give` negative-count behavior

Minecraft cannot store an actual negative-size `ItemStack`: a stack with count `0` or less is empty. NaN Lab therefore assigns an explicit meaning to negative `/give` counts:

- `/give @s minecraft:dirt -1` removes up to one matching dirt from each target inventory.
- `/give @s minecraft:dirt -64` removes up to 64 matching dirt from each target inventory.
- `/give @s minecraft:dirt NaN` resolves to zero and changes nothing.
- `/give @s minecraft:dirt -inf` removes as much matching dirt as is present (the parsed int minimum is safely widened before negation).

Matching includes the item's data components. This is an intentional mod behavior, not vanilla `/give` semantics.

### `/enchant` extended levels

Vanilla enchantment levels are integer-backed and the stock `/enchant` command normally enforces the enchantment's declared maximum. NaN Lab intercepts out-of-range `/enchant` requests and stores a virtual level on the item in `minecraft:custom_data`:

- `/enchant @s minecraft:sharpness 100000` stores virtual level `100000`.
- `/enchant @s minecraft:knockback -1` stores virtual level `-1`.
- `/enchant @s minecraft:knockback NaN` stores a virtual NaN marker.
- `/enchant @s minecraft:knockback inf` stores a virtual positive-infinity marker.
- `/enchant @s minecraft:knockback -inf` stores a virtual negative-infinity marker.

To keep the enchantment present in the item's normal enchantment component, the mod writes a base marker level of `1`. The common `EnchantmentHelper.getItemEnchantmentLevel` API reads the virtual override. Since that API returns an `int`, it converts NaN to `0`, positive infinity to `Integer.MAX_VALUE`, negative infinity to `Integer.MIN_VALUE`, and saturates finite values to the int range. The tooltip can therefore still show the base marker rather than the virtual value, and enchantment effects that bypass the common helper or calculate from the raw component are not guaranteed to use the override. This is not equivalent to storing a true IEEE NaN in an enchantment level (which Minecraft's integer level format cannot represent).

## Player commands

- `/nan set <field> <value>` — apply a numeric value to yourself.
- `/nan set <player> <field> <value>` — apply a numeric value to an online player.
- `/nan reset [player]` — restore safe player values and move the player to the current dimension's respawn position.
- `/nan list` and `/nan help` — list supported fields and syntax.

Accepted values: `nan`, `inf`, `-inf`, `infinity`, `-infinity`, or a Java floating-point number such as `0.25`, `-3.5`, or `1.0E300`.

### Player fields

| Field | Underlying value |
|---|---|
| `x`, `y`, `z` | double position coordinates |
| `motion_x`, `motion_y`, `motion_z` | double velocity components |
| `yaw`, `pitch` | float rotation values |
| `fall_distance` | float fall distance |
| `walk_speed`, `fly_speed` | float ability speed fields |
| `movement_speed`, `armor`, `armor_toughness`, `attack_damage`, `attack_speed`, `knockback_resistance`, `max_health`, `luck` | double attribute base values |
| `health`, `absorption` | float health / absorption values |
| `saturation`, `exhaustion` | float food values |
| `xp_progress` | float XP-bar progress |

Groups: `position`, `motion`, `rotation`, `speeds`, `attributes`, `vitals`, `hunger`, and `all`.

Examples:

```text
/nan set x nan
/nan set motion_y nan
/nan set rotation nan
/nan set attack_speed infinity
/nan set knockback_resistance -infinity
/nan set health nan
/nan set attributes nan
/nan set all nan
/nan set Notch health nan
/nan reset
```

## Held-item floating-point effect overrides

These `/nan enchant <effect> <value>` commands apply an additional floating-point calculation override to the item in your **main hand**. The override is stored in that item's `minecraft:custom_data`, so it travels with the stack and persists through saves. The vanilla `/enchant` command now has separate virtual-level support described above.

- `/nan enchant <effect> <value>` — set an effect override.
- `/nan effect <effect> <value>` — alias for the same command.
- `/nan enchant clear` — clear every NaN Lab effect override from the held item.
- `/nan enchant clear <effect>` — clear one override.

Currently supported effect hooks:

| Effect | Calculation being overridden |
|---|---|
| `knockback` | `EnchantmentHelper.modifyKnockback` |
| `damage` | `EnchantmentHelper.modifyDamage` |
| `fall_damage` | `EnchantmentHelper.modifyFallBasedDamage` |
| `armor_effectiveness` | `EnchantmentHelper.modifyArmorEffectiveness` |
| `crossbow_charge_time` | `EnchantmentHelper.modifyCrossbowChargingTime` |
| `projectile_spread` | `EnchantmentHelper.processProjectileSpread` |

Examples:

```text
/give @s minecraft:netherite_sword
/nan enchant knockback nan
/nan enchant knockback 100000
/nan enchant damage infinity
/nan enchant clear knockback
/nan enchant clear
```

**Important distinction:** vanilla enchantment levels are integers, so an enchantment level itself cannot be NaN or Infinity. The `/nan enchant` command is a per-item override of the resulting floating-point calculation, not a rewrite of the enchantment's integer level or tooltip. For example, it can make the held item return `NaN` from the knockback calculation even though the sword's visible Knockback tooltip is unchanged. The override is active on that stack whether or not the normal enchantment level is present; it directly overrides the selected helper calculation.

The game calculation methods listed above use 32-bit `float` values. `NaN` and infinities are preserved; finite values outside the float range overflow to positive or negative infinity. Values such as `100000` are represented normally.

## Mod Menu

Install Mod Menu 21.0.0 for Minecraft 26.3 to access the in-game control panel. The panel includes common player experiments, held-item knockback/damage overrides, and a clear button.

## Permissions and recovery

- Commands require cheats in singleplayer or permission level 2 (operator) on a server.
- This mod mutates values on the server through ordinary commands. It does not send malformed packets or bypass server permissions.
- Back up worlds before experimenting. Applying NaN or Infinity to position, motion, health, or combat calculations can cause unusual behavior or make recovery difficult.
- `/nan reset` restores a set of safe player defaults. It does not clear held-item effect overrides; use `/nan enchant clear` while holding the affected item.
- Some player values can be overwritten by normal game ticks, attributes, other mods, or server logic. Results can vary.

## Build

Use JDK 25 and the Gradle version configured in `.github/workflows/gradle.yml`. From this folder:

```bash
gradle --no-daemon clean build
```

The output JAR will be in `build/libs/`. Install the JAR and Fabric API in the Fabric `mods` folder. Mod Menu is optional.

## Implementation notes

- Player attributes and motion/position fields use native Minecraft APIs where available.
- The mixin layer bypasses standard Brigadier numeric min/max checks, extends `StringReader` numeric reads, handles negative `/give` counts as inventory removal, intercepts out-of-range `/enchant` levels as per-item virtual values, and hooks selected float-returning `EnchantmentHelper` calculations.
- Integer-only fields (for example, item durability and enchantment levels) cannot natively store IEEE NaN. Those require a dedicated calculation hook if a NaN-like runtime effect is desired.
- Some player speed, fall-distance, exhaustion, and XP fields are accessed reflectively to tolerate minor implementation-name differences; the command reports a failure if a field cannot be located.
