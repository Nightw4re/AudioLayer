package com.audiolayer;

import com.audiolayer.api.AudiolayerProvider;
import com.audiolayer.api.ClientAudiolayerApi;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class AudiolayerClientSetup {
    private static final Logger LOGGER = LogUtils.getLogger();

    public AudiolayerClientSetup(IEventBus modEventBus, AudiolayerManager manager, Path packPath) {
        ClientAudiolayerApi api = new ClientAudiolayerApi(manager);
        AudiolayerProvider.register(api);
        modEventBus.addListener((AddPackFindersEvent event) -> onAddPackFinders(event, packPath));
    }

    private void onAddPackFinders(AddPackFindersEvent event, Path packPath) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) return;
        if (!Files.exists(packPath)) {
            LOGGER.warn("Audiolayer runtime pack not found at {}", packPath.toAbsolutePath());
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    new PackLocationInfo("audiolayer-runtime", Component.literal("Audiolayer Runtime"), PackSource.BUILT_IN, Optional.empty()),
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo info) {
                            return new PathPackResources(info, packPath);
                        }
                        @Override
                        public net.minecraft.server.packs.PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                            return new PathPackResources(info, packPath);
                        }
                    },
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(true, Pack.Position.TOP, false)
            );
            if (pack != null) consumer.accept(pack);
        });
        LOGGER.info("Audiolayer runtime pack registered");
    }

}
