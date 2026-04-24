package com.audiolayer;

import com.audiolayer.audio.HashService;
import com.audiolayer.audio.InputAudioScanner;
import com.audiolayer.audio.Mp3StreamDecoder;
import com.audiolayer.cache.JsonCacheIndexRepository;
import com.audiolayer.commands.AudiolayerServerCommands;
import com.audiolayer.config.AudiolayerConfig;
import com.audiolayer.config.FilenameSanitizer;
import com.audiolayer.config.SoundIdMapper;
import com.audiolayer.network.AudiolayerClientHandler;
import com.audiolayer.network.AudiolayerPlayPacket;
import com.audiolayer.network.AudiolayerStopPacket;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(AudiolayerMod.MODID)
public final class AudiolayerMod {
    public static final String MODID = "audiolayer";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void extractSampleIfEmpty(Path inputDir) {
        try {
            Files.createDirectories(inputDir);
            try (var stream = Files.list(inputDir)) {
                if (stream.findAny().isPresent()) return;
            }
            Path target = inputDir.resolve("sample.mp3");
            try (InputStream in = AudiolayerMod.class.getResourceAsStream("/audiolayer/sample.mp3")) {
                if (in != null) {
                    Files.copy(in, target);
                    LOGGER.info("Audiolayer: extracted sample.mp3 to {}", target.toAbsolutePath());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Audiolayer: could not extract sample: {}", e.getMessage());
        }
    }

    public AudiolayerMod(IEventBus modEventBus) {
        LOGGER.info("Audiolayer loaded");
        extractSampleIfEmpty(Path.of("config", "audiolayer", "input"));
        AudiolayerConfig config = new AudiolayerConfig(
                Path.of("config", "audiolayer", "input"),
                Path.of("config", "audiolayer", "cache"),
                true
        );
        AudiolayerManager manager = new AudiolayerManager(
                config,
                new InputAudioScanner(new SoundIdMapper(new FilenameSanitizer()), new HashService()),
                new JsonCacheIndexRepository(config.cacheDirectory().resolve("index.json")),
                Mp3StreamDecoder::readDurationSeconds
        );
        manager.reload();

        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> {
            PayloadRegistrar reg = event.registrar("2");
            reg.playToClient(AudiolayerPlayPacket.TYPE, AudiolayerPlayPacket.CODEC,
                    (pkt, ctx) -> ctx.enqueueWork(() -> AudiolayerClientHandler.onPlay(pkt)));
            reg.playToClient(AudiolayerStopPacket.TYPE, AudiolayerStopPacket.CODEC,
                    (pkt, ctx) -> ctx.enqueueWork(() -> AudiolayerClientHandler.onStop(pkt)));
        });

        NeoForge.EVENT_BUS.register(new AudiolayerServerCommands(manager));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new AudiolayerClientSetup(modEventBus, manager);
        }
    }
}
