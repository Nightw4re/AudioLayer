# Audiolayer

Audiolayer is a NeoForge 1.21.1 mod that streams MP3 files directly from disk and plays them via OpenAL under the `audiolayer` namespace. No conversion, no resource pack — just drop in an MP3 and play it.

## Table of Contents

- [Installation](#installation)
- [Adding Sounds](#adding-sounds)
- [Sound IDs](#sound-ids)
- [In-Game Commands](#in-game-commands)
- [Server Support](#server-support)
- [Mod Integration API](#mod-integration-api)
- [Folder Layout](#folder-layout)
- [Development](#development)

---

## Installation

1. Install NeoForge 1.21.1.
2. Drop `audiolayer-neoforge-<version>.jar` into your `mods/` folder.
3. Start Minecraft — the mod creates the input folder and extracts a sample MP3 automatically.

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

Run `/audiolayer reload` to pick up changes without restarting.

---

## Sound IDs

Each file is mapped to a `namespace:path` ID:

| File path (relative to input/) | Sound ID |
|---|---|
| `sample.mp3` | `audiolayer:sample` |
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

All commands require OP level 2 on a server. In singleplayer they are available to all players.

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
| `start` | float ≥ 0 | `0` | Start position in seconds |
| `duration` | float ≥ 0 | `0` | Duration per repetition in seconds. `0` = until end of file |

Examples:

```
# Play the built-in sample once
/audiolayer play audiolayer:sample

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

Rescans the input folder and updates the duration cache. Already-known files are reused.

---

## Server Support

Audiolayer commands work on a multiplayer server. The server sends a network packet to the client that issued the command — the sound plays on that client only.

**Requirement:** every player who wants to hear Audiolayer sounds must have the mod installed with the same MP3 files in their `config/audiolayer/input/` folder (or a modpack that ships those files).

The server itself never plays audio; it only routes commands to the right client.

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

    /** Returns metadata for the sound (source path, hash, duration). */
    Optional<LoadedAudioAsset> get(SoundId id);

    /** Rescans the input folder and updates the cache. */
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

// To string
id.toString(); // "audiolayer:music.theme"
```

### LoadedAudioAsset

```java
api.get(id).ifPresent(asset -> {
    Path mp3 = asset.sourceFile();     // original MP3 path
    float duration = asset.durationSeconds();
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
    input/        ← place your MP3 files here (sample.mp3 is extracted on first run)
    cache/
      index.json  ← tracks file hashes and durations across restarts
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

**Stack:** NeoForge 1.21.1 · Minecraft 1.21.1 · Java 21 · JLayer 1.0.1 (MP3 streaming)
