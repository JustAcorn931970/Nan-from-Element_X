# Fix for the 26.3 GitHub Actions compile log

The uploaded Actions log was compiling older versions of `NanLabConfigScreen.java` and `NanLabMod.java`. This project package contains the 26.3 API replacements for every error shown in that log.

| Old API / error | Replacement in this source |
|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `Screen.render(...)` | `Screen.extractRenderState(...)` |
| `Minecraft.setScreen(...)` | `Minecraft.gui.setScreen(...)` |
| `CommandSourceStack.hasPermission(2)` | `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` |
| `GameProfile.getName()` | `GameProfile.name()` |
| `FoodData.setExhaustion(...)` | reflective write to the 26.3 `exhaustionLevel` field |
| `ServerPlayer.serverLevel()` | `ServerPlayer.level()` |
| `ServerLevel.getSharedSpawnPos()` | `ServerLevel.getRespawnData().pos()` |

## Important: update the repository correctly

1. Download and extract this ZIP.
2. Open the inner `nan-lab` folder.
3. Copy **everything inside** `nan-lab` into the root of your GitHub repository, including `src`, `build.gradle`, `gradle.properties`, `settings.gradle`, and `.github`.
4. Allow GitHub to replace the old files. Do not copy only the Java files and do not put the inner `nan-lab` folder inside another nested project folder.
5. Commit and push, then run **Actions → Build NaN (Fabric 26.3) → Run workflow**.

The Gradle plugin is pinned to Loom 1.19.3, the version reported by the supplied Actions log. The project targets Java 25.

## Verification status

This archive was checked for ZIP integrity and for the obsolete API names listed above. It has **not** been compiled here: this environment has Java 21 and no Gradle installation. A successful GitHub Actions run is still required before treating the JAR as build-verified.
