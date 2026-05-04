package com.audiolayer.audio;

import com.audiolayer.config.SoundIdMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ResourcePackAudioScanner {
    private static final String RESOURCE_PREFIX = "audiolayer/sounds";
    private final SoundIdMapper mapper;
    private final HashService hashService;
    private final Path extractionDirectory;

    public ResourcePackAudioScanner(SoundIdMapper mapper, HashService hashService, Path extractionDirectory) {
        this.mapper = mapper;
        this.hashService = hashService;
        this.extractionDirectory = extractionDirectory;
    }

    public List<AudioSourceDescriptor> scan(Object resourceManager) throws IOException {
        List<AudioSourceDescriptor> descriptors = new ArrayList<>();
        Map<?, ?> resources = invokeListResources(resourceManager);
        for (var entry : resources.entrySet()) {
            descriptors.add(descriptor(entry.getKey(), entry.getValue()));
        }
        return descriptors;
    }

    private Map<?, ?> invokeListResources(Object resourceManager) throws IOException {
        try {
            Method method = resourceManager.getClass().getMethod("listResources", String.class, Predicate.class);
            Object result = method.invoke(resourceManager, RESOURCE_PREFIX, (Predicate<Object>) id ->
                    id.toString().toLowerCase().endsWith(".mp3"));
            return (Map<?, ?>) result;
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to list resource-pack MP3 files", e);
        }
    }

    private AudioSourceDescriptor descriptor(Object resourceLocation, Object resource) throws IOException {
        String id = resourceLocation.toString();
        String relativePath = id.substring(id.indexOf(RESOURCE_PREFIX) + RESOURCE_PREFIX.length() + 1);
        String hash;
        try (InputStream in = open(resource)) {
            hash = hashService.hash(in);
        }
        Path extracted = extractionDirectory.resolve(hash + "-" + sanitizeFileName(Path.of(relativePath).getFileName().toString()));
        Files.createDirectories(extractionDirectory);
        if (!Files.exists(extracted)) {
            try (InputStream in = open(resource)) {
                Files.copy(in, extracted);
            }
        }
        Path relative = Path.of(relativePath);
        String fileName = relative.getFileName().toString();
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String category = relative.getParent() == null ? "" : relative.getParent().toString().replace('\\', '/');
        return new AudioSourceDescriptor(
                extracted.toAbsolutePath(),
                relativePath,
                "mp3",
                Files.size(extracted),
                hash,
                category,
                base,
                mapper.map(relative)
        );
    }

    private InputStream open(Object resource) throws IOException {
        try {
            return (InputStream) resource.getClass().getMethod("open").invoke(resource);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Failed to open resource-pack MP3", e);
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
