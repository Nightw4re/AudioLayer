package com.audiolayer.resource;

import com.audiolayer.audio.LoadedAudioAsset;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class RuntimeResourcePackWriter {
    private final Path packRoot;

    public RuntimeResourcePackWriter(Path packRoot) {
        this.packRoot = packRoot;
    }

    public void write(Collection<LoadedAudioAsset> assets) throws IOException {
        Files.createDirectories(packRoot.resolve("assets/audiolayer/sounds"));

        Map<String, String> soundMap = new LinkedHashMap<>();
        for (LoadedAudioAsset asset : assets) {
            if (!Files.exists(asset.cacheFile())) {
                continue;
            }
            String path = asset.soundId().path();
            String targetName = path.replace('.', '_');
            Path target = packRoot.resolve("assets/audiolayer/sounds").resolve(targetName + ".ogg");
            Files.createDirectories(target.getParent());
            Files.copy(asset.cacheFile(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            soundMap.put(path, "{\"sounds\":[{\"name\":\"audiolayer:" + targetName + "\",\"preload\":true}]}");
        }

        String soundsJson = soundMap.entrySet().stream()
                .map(entry -> "\"" + entry.getKey() + "\": " + entry.getValue())
                .collect(Collectors.joining(",\n", "{\n", "\n}\n"));
        Files.writeString(packRoot.resolve("assets/audiolayer/sounds.json"), soundsJson, StandardCharsets.UTF_8);
        Files.writeString(packRoot.resolve("pack.mcmeta"),
                "{\n  \"pack\": {\n    \"pack_format\": 46,\n    \"description\": \"Audiolayer runtime audio pack\"\n  }\n}\n",
                StandardCharsets.UTF_8);
    }
}
