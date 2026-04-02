package com.audiolayer.cache;

import java.io.IOException;

public interface CacheIndexRepository {
    CacheIndex load() throws IOException;

    void save(CacheIndex index) throws IOException;
}
