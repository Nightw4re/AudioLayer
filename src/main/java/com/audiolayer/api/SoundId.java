package com.audiolayer.api;

import java.util.Objects;

/**
 * Stable sound identifier in the form namespace:path.
 */
public record SoundId(String namespace, String path) {
    public SoundId {
        namespace = Objects.requireNonNull(namespace, "namespace");
        path = Objects.requireNonNull(path, "path");
        if (namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("SoundId parts must not be blank");
        }
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
