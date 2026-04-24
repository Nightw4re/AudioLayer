# Manual Test Scenarios

API contract, reload logic, and play/stop behaviour are covered automatically by `CompatApiContractTest` — run with `npm test`.
KubeJS binding (play/stop/isLoaded/listSounds) is covered automatically by `AudiolayerJSWrapperTest`.

The scenarios below require a running game and can only be tested manually.

---

## Scenario 1 — KubeJS gives a pre-configured Etched disc

**Goal:** Verify the KubeJS `Audiolayer` binding works and can give a ready-to-use disc.
KubeJS script: `runs/compat/etched/kubejs/server_scripts/audiolayer_test.js`

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Run `/audiolayer_test disc`
4. **Expected:** you receive a jukebox and an Etched disc named *Audiolayer Sample*
5. Place the jukebox, insert the disc
6. **Expected:** MP3 starts playing via Audiolayer hook
7. Run `/audiolayer_test stop`
8. **Expected:** playback stops

---

## Scenario 2 — KubeJS plays and stops directly

**Goal:** Verify `Audiolayer.play()` and `Audiolayer.stop()` work from a KubeJS server script.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Run `/audiolayer_test play`
4. **Expected:** MP3 starts playing
5. Run `/audiolayer_test stop`
6. **Expected:** playback stops immediately

---

## Scenario 3 — Etched disc plays MP3 via hook (manual etching)

**Goal:** Verify that placing an Etched disc configured with an `audiolayer:` sound event
into a jukebox triggers the PlaySoundEvent hook and plays the MP3.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Run `/audiolayer list` — confirm `audiolayer:sample` appears
4. Run `/audiolayer test setup` — gives you a jukebox, blank music disc, and music label
5. Place the **Etching Table** (craft it or use `/give @s etched:etching_table`)
6. Open the Etching Table, insert the blank disc and music label
7. In the URL/sound event field enter: `audiolayer:sample`
8. Take the finished Etched Music Disc
9. Place the jukebox, insert the disc
10. **Expected:** MP3 starts playing
11. Check the log for: `Audiolayer intercepted sound event: audiolayer:sample`

---

## Scenario 4 — Unknown sound is silently ignored

**Goal:** Verify the hook does not crash when the sound ID is not loaded.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Create an Etched disc with sound event: `audiolayer:does_not_exist`
4. Place the jukebox, insert the disc
5. **Expected:** nothing plays, no crash, no error in the log

---

## Scenario 5 — Playback stops when disc is removed

**Goal:** Verify there is no ghost audio after the disc is ejected.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Run `/audiolayer test setup`
4. Create an Etched disc with `audiolayer:sample`, insert into jukebox
5. Confirm MP3 is playing (Scenario 1)
6. Right-click the jukebox to eject the disc
7. **Expected:** playback stops immediately

---

## Scenario 6 — Clean startup with Etched

**Goal:** Verify no errors or crashes on startup with Etched present.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. **Expected:** no `ERROR` or `FATAL` lines from `audiolayer` in the log, no crash screen

---

## Scenario 7 — Debug command returns correct metadata

**Goal:** Verify the sound file is correctly loaded and its metadata is readable.

1. Run `npm run compat:etched`
2. Create a singleplayer world
3. Run:
   ```
   /audiolayer debug audiolayer:sample
   ```
4. **Expected:** response shows `source=ok`, a non-zero duration, and a valid file path
