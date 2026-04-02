# Audiolayer

Audiolayer is a NeoForge 1.21.1 mod that loads MP3 files from a folder, converts them to OGG, and exposes them as playable Minecraft sound assets under the `audiolayer` namespace.

## Table of Contents

- [Installation](#installation)
- [Adding Sounds](#adding-sounds)
- [Sound IDs](#sound-ids)
- [In-Game Commands](#in-game-commands)
- [Server Support](#server-support)
- [Resource Pack](#resource-pack)
- [Mod Integration API](#mod-integration-api)
- [Folder Layout](#folder-layout)
- [Development](#development)

---

## Installation

1. Install NeoForge 1.21.1.
2. Drop `audiolayer-0.1.0.jar` into your `mods/` folder.
3. Start Minecraft once — the mod creates the input folder automatically.

---

## Adding Sounds

Place **MP3** files into:

```
config/audiolayer/input/
```

Subfolders become part of the sound ID. Example structure:

```
config/audiolayer/input/
  music/
    theme.mp3
    boss_fight.mp3
  ambience/
    cave_loop.mp3
  ui/
    confirm.mp3
```

Restart Minecraft or run `/audiolayer reload` to pick up changes.

---

## Sound IDs

Each file is mapped to a `namespace:path` ID:

| File path (relative to input/) | Sound ID |
|---|---|
| `music/theme.mp3` | `audiolayer:music.theme` |
| `ambience/cave_loop.mp3` | `audiolayer:ambience.cave_loop` |
| `ui/confirm.mp3` | `audiolayer:ui.confirm` |

Rules:
- Folder separators (`/`) become `.` in the path.
- Spaces are replaced with `_`.
- Everything is lowercased.
- The file extension is stripped.

---

## In-Game Commands

All commands are available to OPs on a server and to any player in singleplayer.

### List loaded sounds

```
/audiolayer list
```

### Play a sound

```
/audiolayer play <sound_id> [count] [start] [duration]
```

| Argument | Type | Default | Meaning |
|---|---|---|---|
| `sound_id` | ResourceLocation | — | Sound to play (tab-complete supported) |
| `count` | integer ≥ 0 | `1` | Number of repetitions. `0` = infinite loop |
| `start` | float ≥ 0 | `0` | Start position in seconds. `0` = beginning |
| `duration` | float ≥ 0 | `0` | Duration per repetition in seconds. `0` = until end of file |

Examples:

```
# Play once from the beginning
/audiolayer play audiolayer:music.theme

# Play 3 times
/audiolayer play audiolayer:music.theme 3

# Loop infinitely from second 10
/audiolayer play audiolayer:music.theme 0 10

# Play twice, starting at 5 s, playing 30 s each time
/audiolayer play audiolayer:music.theme 2 5 30
```

### Stop playback

```
/audiolayer stop
```

### Reload sounds

```
/audiolayer reload
```

Rescans the input folder and rebuilds the cache. Already-cached files are reused unless their content changed.

---

## Server Support

Audiolayer commands work on a multiplayer server. The server sends network packets to the client that issued the command — the sound plays on the client only, using the client's local file cache.

**Requirement:** every player who wants to hear Audiolayer sounds must have the mod installed with the same audio files in their `config/audiolayer/input/` folder (or a modpack that ships those files).

The server itself never plays audio; it only routes commands to the right client.

---

## Resource Pack

On first run the mod writes a runtime resource pack to:

```
resourcepacks/audiolayer-runtime/
```

The pack is registered automatically — no manual activation is needed.  
If sounds are not playing, open **Options → Resource Packs** and ensure **Audiolayer Runtime** is active, then click **Done** to reload resources.

---

## Mod Integration API

Other mods can interact with Audiolayer through a stable Java API without depending on internal classes.

### Dependency

Add Audiolayer as a `compileOnly` dependency and declare it as an optional dependency in `neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
    modId = "audiolayer"
    type = "optional"
    versionRange = "[0.1,)"
    ordering = "AFTER"
    side = "CLIENT"
```

### Obtaining the API

```java
import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.SoundId;

Optional<AudiolayerApi> api = AudiolayerProvider.get();
```

The instance is available after Audiolayer's mod constructor runs. It is safe to store the reference.

### AudiolayerApi

```java
public interface AudiolayerApi {

    /** True if the sound is loaded and ready to play. */
    boolean isLoaded(SoundId id);

    /** All currently loaded sound IDs. */
    Set<SoundId> listSounds();

    /**
     * Returns the loaded asset, which exposes the path to the converted OGG file.
     * Other mods can use this path for their own playback pipeline.
     */
    Optional<LoadedAudioAsset> get(SoundId id);

    /** Rescans the input folder and rebuilds the runtime resource pack. */
    void reload();

    /** Plays the sound once from the beginning. */
    void play(SoundId id);

    /**
     * Plays the sound with full control.
     *
     * @param count           repetitions (0 = infinite)
     * @param startSeconds    start position in seconds (0 = beginning)
     * @param durationSeconds duration per repetition (0 = to end of file)
     */
    void play(SoundId id, int count, float startSeconds, float durationSeconds);

    /** Stops the currently playing Audiolayer sound. */
    void stop();
}
```

### SoundId

```java
// Construct
SoundId id = new SoundId("audiolayer", "music.theme");

// From a string
String[] parts = "audiolayer:music.theme".split(":", 2);
SoundId id = new SoundId(parts[0], parts[1]);

// To string
id.toString(); // "audiolayer:music.theme"
```

### LoadedAudioAsset

```java
api.get(id).ifPresent(asset -> {
    Path oggFile = asset.cacheFile();   // path to the converted OGG
    float duration = asset.durationSeconds();
    // hand off oggFile to your own audio pipeline
});
```

### Example: play if loaded

```java
AudiolayerProvider.get().ifPresent(api -> {
    SoundId id = new SoundId("audiolayer", "music.theme");
    if (api.isLoaded(id)) {
        api.play(id, 0, 0f, 0f); // infinite loop from the beginning
    }
});
```

> **Note:** All `play` and `stop` calls must be made from the client thread. Do not call them from server-side code.

---

## Folder Layout

```
config/
  audiolayer/
    input/          ← place your audio files here
    cache/
      index.json    ← tracks which files have been converted
      *.ogg         ← converted audio (reused across restarts)

resourcepacks/
  audiolayer-runtime/   ← generated runtime resource pack (do not edit)
    pack.mcmeta
    assets/
      audiolayer/
        sounds.json
        sounds/
          *.ogg
```

---

## Development

```bash
# Run unit tests (no Minecraft required)
npm test

# Build the mod jar
npm run build

# Start NeoForge client in dev
./gradlew runClient
```

**Stack:** NeoForge 1.21.1 · Minecraft 1.21.1 · Java 21 · JAVE2 3.4.0 (bundled ffmpeg for audio conversion)
