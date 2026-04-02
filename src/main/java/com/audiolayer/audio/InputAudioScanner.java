package com.audiolayer.audio;

import com.audiolayer.config.SoundIdMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class InputAudioScanner {
    private final SoundIdMapper mapper;
    private final HashService hashService;

    public InputAudioScanner(SoundIdMapper mapper, HashService hashService) {
        this.mapper = mapper;
        this.hashService = hashService;
    }

    public List<AudioSourceDescriptor> scan(Path inputDirectory) throws IOException {
        List<AudioSourceDescriptor> descriptors = new ArrayList<>();
        if (!Files.exists(inputDirectory)) {
            return descriptors;
        }
        try (var stream = Files.walk(inputDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isMp3)
                    .forEach(path -> descriptors.add(descriptor(inputDirectory, path)));
        }
        return descriptors;
    }

    private boolean isMp3(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".mp3");
    }

    private AudioSourceDescriptor descriptor(Path root, Path file) {
        Path relative = root.relativize(file);
        String relativePath = relative.toString().replace('\\', '/');
        String hash;
        try {
            hash = hashService.hash(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String fileName = file.getFileName().toString();
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String category = relative.getParent() == null ? "" : relative.getParent().toString().replace('\\', '/');
        return new AudioSourceDescriptor(
                file.toAbsolutePath(),
                relativePath,
                "mp3",
                file.toFile().length(),
                hash,
                category,
                base,
                mapper.map(relative)
        );
    }
}
