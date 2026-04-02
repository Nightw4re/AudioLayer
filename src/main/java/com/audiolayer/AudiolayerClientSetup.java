package com.audiolayer;

import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.ClientAudiolayerApi;
import com.audiolayer.registry.AudiolayerManager;
import net.neoforged.bus.api.IEventBus;

public final class AudiolayerClientSetup {

    public AudiolayerClientSetup(IEventBus modEventBus, AudiolayerManager manager) {
        ClientAudiolayerApi api = new ClientAudiolayerApi(manager);
        AudiolayerProvider.register(api);
    }
}
