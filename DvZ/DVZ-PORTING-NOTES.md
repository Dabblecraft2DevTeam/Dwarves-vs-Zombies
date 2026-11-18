# Dwarves vs Zombies (DvZ) Porting Notes: Minecraft 1.12.2 to 1.21.4 (Paper)

## 1. Overview
DvZ has been migrated from Bukkit 1.12.2 (with legacy NMS reflection targeting `net.minecraft.server.v1_7_R1`) to Paper 1.21.4 on Java 21 (`paper-api:1.21.4-R0.1-SNAPSHOT`). All 122 Java source files compile cleanly with zero errors under Java 21.

This concludes Phase 2 of the DvZ modernisation plan.

---

## 2. Dependency Swaps

| Component | Legacy (1.12.2 / 1.7 era) | Modern (1.21.4 / Paper) | Notes |
|---|---|---|---|
| **Platform API** | `org.bukkit:bukkit:1.12.2-R0.1-SNAPSHOT` | `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT` | Repository: `https://repo.papermc.io/repository/maven-public/`; scope `provided` |
| **Java Target** | Java 1.8 (`source=1.8`, `target=1.8`) | Java 21 (`<release>21</release>`) | `maven-compiler-plugin:3.13.0` |
| **SpellItems** | Dead Dropbox link (`andre111-repo`) | `me.andre111:SpellItems:2.2.0` | Sourced from local Maven cache (`~/.m2/repository/me/andre111/SpellItems/2.2.0/`); scope `provided` |
| **ProtocolLib** | `com.comphenix.protocol:ProtocolLib-API:4.3.0` | `net.dmulloy2:ProtocolLib:5.4.0` | Sourced from Maven Central; scope `provided` |
| **LibsDisguises** | `LibsDisguises:LibsDisguises:9.0.7` | `me.libraryaddict.disguises:libsdisguises:26.11.10` | Sourced from `https://mvn.lib.co.nz/public/` and cached locally; scope `provided` |
| **Multiverse-Core** | Undeclared compile dependency | `com.onarandombox.multiversecore:Multiverse-Core:4.3.1` | Sourced from OnARandomBox repo / local cache; scope `provided` |
| **Plugin Metrics** | `org.mcstats.metrics:metrics:R7` | **Removed** | Dead mcstats server removed; `Metrics.java` deleted |
| **DisguiseCraft** | Dead soft-dependency | **Removed** | `DSystem_DisguiseCraft.java` deleted |
| **BeTheMob** | Dead commented references | **Removed** | Removed references in `SupportedDisguises.java` |

---

## 3. Plugin Descriptor (`plugin.yml`)
- Added `api-version: '1.21'`.
- Updated soft-dependencies to `softdepend: [LibsDisguises, ProtocolLib, SpellItems, Multiverse-Core]`.
- All commands, permission nodes, aliases, descriptions, and original author attribution (`andre111`) are fully preserved.

---

## 4. Removed and Replaced NMS Hacks (`volatileCode/`)

### 4.1 `DynamicClassFunctions.java`
- **Before**: Used reflection into `net.minecraft.server.v1_7_R1` and `org.bukkit.craftbukkit.v1_7_R1` (`CraftWorld`, `CraftServer`, `CraftFallingSand`, `RegionFileCache`, `RegionFile`, `EntityFallingBlock`). When resetting worlds, it manually reflected into private `RegionFileCache` maps to close underlying `RandomAccessFile` file handles, and forcibly stripped worlds from internal server world lists.
- **After**:
  - `forceUnloadWorld(World world)`: Calls modern `Bukkit.unloadWorld(world, false)` after kicking players with the reset message.
  - `bindRegionFiles()`, `unbindRegionFiles()`, and `clearWorldReference()`: Replaced with safe no-ops.
  - `setFallingBlockHurtEntities(FallingBlock, float damage, int max)`: Replaced with official Bukkit API methods (`block.setHurtEntities(true)`, `block.setDamagePerBlock(damage)`, `block.setMaxDamage(max)`).
  - Reflection package setup (`setPackages()`, `setClasses()`, `setMethods()`, `setFields()`) preserved as safe no-ops returning `true`.

### 4.2 `DvZPackets.java`
- **BossBar / InfoBar**:
  - **Before**: Sent raw fake `SPAWN_ENTITY_LIVING` packets spawning an invisible Ender Dragon 256 blocks below the player with custom name and health metadata.
  - **After**: Migrated to the official Bukkit `BossBar` API (`Bukkit.createBossBar(name, BarColor.PURPLE, BarStyle.SOLID)`). Automatically tracks and updates progress smoothly per player UUID.
- **Particle Spawning**:
  - **Before**: Handled via ProtocolLib `WORLD_PARTICLES` raw packet crafting.
  - **After**: Replaced with standard Bukkit `World#spawnParticle` and a legacy particle name mapping helper.
- **Block Break Animations**:
  - **Before**: Sent raw `BLOCK_BREAK_ANIMATION` packets with legacy integer position packing.
  - **After**: Replaced with standard Bukkit `Player#sendBlockDamage(Location, float progress, int entityId)`.
- **Titles**:
  - Replaced legacy `TITLE` packet dispatch with Bukkit `Player#sendTitle(title, subtitle, fadeIn, stay, fadeOut)`.
- **Entity Interaction Listener**:
  - Modernized `USE_ENTITY` packet listening for ProtocolLib 5.4.0, reading `EntityUseAction` and `WrappedEnumEntityUseAction` across attack and interact events.
- **Fake Zombie Item Stands**:
  - `sendFakeFloatingZombieSpawn` and `sendFakeZombieRemove`: Updated to modern ProtocolLib 5.4.0 packet types (`PacketType.Play.Server.SPAWN_ENTITY` with UUID and double coordinates, and `PacketType.Play.Server.ENTITY_DESTROY` with integer list).

### 4.3 `DeprecatedMethods.java`
- Updated `getMaterialByID`, `setBlockID`, `setBlockData`, `setBlockIDandData`, `spawnFallingBlock`, and `sendBlockChange` to convert legacy IDs and byte data using `Bukkit.getUnsafe().fromLegacy(...)` and modern `BlockData`.
- `getMaterialFromInternalName` and `tabCompleteInternalName` updated to match non-legacy material namespaces.

---

## 5. Disguise Subsystem Migration (`disguise/`)
- Deleted dead `DSystem_DisguiseCraft.java`.
- Trimmed `SupportedDisguises.java` and `DisguiseSystemHandler.java` to retain only LibsDisguises (disabling gracefully if LibsDisguises is not installed).
- Modernized `DvZDisguiseType.java`:
  - `VILLAGER_ZOMBIE` -> `EntityType.ZOMBIE_VILLAGER`
  - `WITHER_SKELETON` -> `EntityType.WITHER_SKELETON`
  - `ZOMBIE_PIGMAN` -> `EntityType.ZOMBIFIED_PIGLIN`
  - `UNDEAD_HORSE` -> `EntityType.ZOMBIE_HORSE`
  - `SKELETON_HORSE` -> `EntityType.SKELETON_HORSE`
  - `MOOSHROOM` -> `EntityType.MOOSHROOM`
  - `SNOW_GOLEM` -> `EntityType.SNOW_GOLEM`
- Rewrote `DSystem_LibsDisguises.java` against the modern LibsDisguises 26.x API:
  - Disguise construction migrated from legacy 3-arg constructor to `new MobDisguise(DisguiseType, boolean isAdult)`.
  - Enabled self-disguise visibility using `mdis.setViewSelfDisguise(true)` and `mdis.setHearSelfDisguise(true)`.
  - Configured global default via `DisguiseConfig.setViewSelfDisguisesDefault(true)`.
  - Powered creeper disguise updated to set power via `((CreeperWatcher) mdis.getWatcher()).setPowered(true)`.

---

## 6. Codebase-Wide Fixes (122 Java Files)

### 6.1 World Generation (`ChunkGeneratorDvZ.java` & Populators)
- **`ChunkGeneratorDvZ.java`**:
  - Migrated from obsolete `generate(World, Random, int, int): byte[]` to modern Bukkit `generateChunkData(World, Random, int, int, BiomeGrid): ChunkData`.
  - Populates block states via `ChunkData#setBlock(x, y, z, Material)`.
- **Populators**:
  - `AntiLeavesPopulator`: Checks for leaves using `Tag.LEAVES.isTagged(block.getType())`.
  - `BiomePopulator`: Replaced `GRASS` with `GRASS_BLOCK`, `LONG_GRASS` with `SHORT_GRASS`, `YELLOW_FLOWER` with `DANDELION`, `RED_ROSE` with `POPPY`, `STATIONARY_WATER` with `WATER`, `WATER_LILY` with `LILY_PAD`.
  - `PathPopulator`: Replaced `FENCE` with `OAK_FENCE` and `GRASS` with `GRASS_BLOCK`.
  - `SpawnPopulator`: Replaced `WOOD` with `OAK_PLANKS`, `FENCE` with `OAK_FENCE`, `STATIONARY_WATER` with `WATER`.
  - `TreeLogPopulator`: Replaced `LOG` with `OAK_LOG`, `GRASS` with `GRASS_BLOCK`, `LONG_GRASS` with `SHORT_GRASS` and `TALL_GRASS`.

### 6.2 Material and Potion Effect Overhauls
- `DvZ.java`: Replaced hardcoded legacy material lists in `isPathable` and `transparent` with modern Bukkit `Tag` lookups (`Tag.SAPLINGS`, `Tag.FLOWERS`, `Tag.CROPS`, `Tag.ALL_SIGNS`, `Tag.PRESSURE_PLATES`, `Tag.BUTTONS`, `Tag.RAILS`, `Tag.CARPETS`) and flattened materials.
- `Game.java`:
  - `Material.ENDER_STONE` -> `Material.END_STONE`
  - `Material.ENCHANTMENT_TABLE` -> `Material.ENCHANTING_TABLE`
  - `PotionEffectType.INCREASE_DAMAGE` -> `PotionEffectType.STRENGTH`
  - `PotionEffectType.DAMAGE_RESISTANCE` -> `PotionEffectType.RESISTANCE`
- Dragon attacks (`DragonBite`, `DragonIce`, `DragonSpew`, `DragonThrow`):
  - `PotionEffectType.CONFUSION` -> `PotionEffectType.NAUSEA`
  - `PotionEffectType.SLOW` -> `PotionEffectType.SLOWNESS`
  - `PotionEffectType.SLOW_DIGGING` -> `PotionEffectType.MINING_FATIGUE`
  - `Sound.NOTE_STICKS` -> `Sound.BLOCK_NOTE_BLOCK_HAT`
  - `Material.STATIONARY_LAVA` -> `Material.LAVA`
- Spell items (`ItemPortal.java`, `ItemReinforcePortal.java`):
  - `Material.ENDER_STONE` -> `Material.END_STONE`
  - `Material.GRILLED_PORK` -> `Material.COOKED_PORKCHOP`
  - `PotionEffectType.JUMP` -> `PotionEffectType.JUMP_BOOST`
  - `PotionEffectType.SLOW` -> `PotionEffectType.SLOWNESS`

### 6.3 Inventories, GUIs, and Items
- `IconMenu.java` & `WaitingMenu.java`:
  - `event.getInventory().getTitle()` migrated to `event.getView().getTitle()`.
  - `WaitingMenu#open`: Replaced illegal inventory size `0` with standard valid minimum size `9`.
- `InventoryHandler.java`:
  - `removeItems` and `countItems` now inspect modern item damage via `Damageable` meta rather than deprecated `ItemStack#getData()`.
- `Fireworks.java`:
  - Replaced private field reflection on `FireworkMeta` with official Bukkit `Firework#detonate()`.
  - `EntityType.FIREWORK` updated to `EntityType.FIREWORK_ROCKET`.
- `Listener_Block.java`:
  - Crop growth acceleration rewritten using modern `org.bukkit.block.data.Ageable` block data instead of deprecated `CropState`/`Crops` `MaterialData`.
- `Listener_Player.java`:
  - Migrated `player.getItemInHand()` to `player.getInventory().getItemMainHand()`.

### 6.4 Character Encoding and String Escaping
- Converted non-UTF-8 German characters in comments (`für`, `Gehört`, `hinzufügen`, etc.) across `Game.java`, `BasicGenerator.java`, `QuarryGenerator.java`, and `MonsterTargeted.java` to valid UTF-8.
- Converted literal section signs in `Listener_Player.java` to strict `\u00a7` escapes.

---

## 7. SpellItems Integration Verification
- DvZ compiles cleanly against `me.andre111:SpellItems:2.2.0` (installed in local Maven cache).
- DvZ's spell classes (`ItemClassCheck`, `ItemCrystalStorage`, `ItemDvZClassPoint`, `ItemDVZTeleport`, `ItemPortal`, `ItemPortalTeleport`, `ItemPotions`, `ItemReinforcePortal`, `ItemVariableSetDvZ`, `MonsterTargeted`) extend `ItemSpell` without signature mismatches.
- `Attributes.java` call sites in `Game.java` (using `Attribute.newBuilder()` with UUID, name, amount, type) function seamlessly with SpellItems' modernized Bukkit attributes implementation.

---

## 8. Checklist for Phase 3 (Runtime Smoke Test)
When deploying `DvZ-1.8.jar` and `SpellItems-2.2.0.jar` to a test 1.21.4 Paper server:
1. **Plugin Enablement**: Verify Paper starts without exceptions loading DvZ, SpellItems, ProtocolLib 5.4.0, and LibsDisguises 26.11.10.
2. **World Creation & Reset**: Run `/dvz_createworld` or test match reset. Confirm `Bukkit.unloadWorld(w, false)` successfully releases file locks and allows world directory cleanup.
3. **Chunk Generator**: Confirm `ChunkGeneratorDvZ` generates smooth custom terrain with trees, paths, and populator structures without lighting or chunk generation freezes.
4. **Disguises**: Test class disguise assignment on dwarf/monster classes (zombies, spiders, powered creepers, undead horses) to verify LibsDisguises 26.x correctly displays mob appearances and self-view.
5. **Monument Boss Bar**: Join a match and verify the monument health BossBar appears at the top of the screen and updates as monument enchanting tables take damage.
6. **GUI Menus**: Test `/dvz_vote` and class selection menus to confirm `IconMenu` and `WaitingMenu` open and close without `IllegalArgumentException`.
