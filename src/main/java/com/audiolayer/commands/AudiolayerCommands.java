package com.audiolayer.commands;

import com.audiolayer.api.AudiolayerApi;
import com.audiolayer.api.SoundId;
import com.audiolayer.audio.LoadedAudioAsset;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class AudiolayerCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AudiolayerApi api;

    public AudiolayerCommands(AudiolayerApi api) {
        this.api = api;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("audiolayer")
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    String sounds = api.listSounds().stream()
                                            .map(SoundId::toString)
                                            .collect(Collectors.joining(", "));
                                    if (sounds.isBlank()) {
                                        context.getSource().sendSuccess(() -> Component.literal("No sounds loaded"), false);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.literal("Loaded sounds: " + sounds), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    api.reload();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Reload triggered. Reload Minecraft resources (F3+T) if sounds do not play."
                                    ), false);
                                    return 1;
                                }))
                        .then(Commands.literal("stop")
                                .executes(context -> {
                                    api.stop();
                                    context.getSource().sendSuccess(() -> Component.literal("Playback stopped"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("sound_id", StringArgumentType.greedyString())
                                        .suggests(this::suggestSoundIds)
                                        .executes(context -> {
                                            String raw = StringArgumentType.getString(context, "sound_id");
                                            SoundId id = AudiolayerCommandSupport.parseSoundId(raw);
                                            return api.get(id).map(asset -> {
                                                boolean sourceExists = Files.exists(asset.sourceFile());
                                                boolean cacheExists = Files.exists(asset.cacheFile());
                                                context.getSource().sendSuccess(() -> Component.literal(
                                                        "id=" + asset.soundId()
                                                                + " | source=" + (sourceExists ? "ok" : "missing")
                                                                + " | cache=" + (cacheExists ? "ok" : "missing")
                                                                + " | duration=" + asset.durationSeconds() + "s"
                                                                + " | path=" + asset.sourceFile()
                                                ), false);
                                                return 1;
                                            }).orElseGet(() -> {
                                                context.getSource().sendFailure(Component.literal("Unknown sound: " + raw));
                                                return 0;
                                            });
                                        })))
                        .then(Commands.literal("play")
                                .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                                        .suggests(this::suggestSoundIds)
                                        .executes(context -> {
                                            String soundId = ResourceLocationArgument.getId(context, "sound_id").toString();
                                            return play(context.getSource(), soundId, 1, 0f, 0f);
                                        })
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> {
                                                    String soundId = ResourceLocationArgument.getId(context, "sound_id").toString();
                                                    int count = IntegerArgumentType.getInteger(context, "count");
                                                    return play(context.getSource(), soundId, count, 0f, 0f);
                                                })
                                                .then(Commands.argument("start", FloatArgumentType.floatArg(0f))
                                                        .executes(context -> {
                                                            String soundId = ResourceLocationArgument.getId(context, "sound_id").toString();
                                                            int count = IntegerArgumentType.getInteger(context, "count");
                                                            float start = FloatArgumentType.getFloat(context, "start");
                                                            return play(context.getSource(), soundId, count, start, 0f);
                                                        })
                                                        .then(Commands.argument("duration", FloatArgumentType.floatArg(0f))
                                                                .executes(context -> {
                                                                    String soundId = ResourceLocationArgument.getId(context, "sound_id").toString();
                                                                    int count = IntegerArgumentType.getInteger(context, "count");
                                                                    float start = FloatArgumentType.getFloat(context, "start");
                                                                    float duration = FloatArgumentType.getFloat(context, "duration");
                                                                    return play(context.getSource(), soundId, count, start, duration);
                                                                }))))))
        );
    }

    private int play(net.minecraft.commands.CommandSourceStack source, String soundId, int count, float startSeconds, float durationSeconds) {
        SoundId id = AudiolayerCommandSupport.parseSoundId(soundId);
        if (!api.isLoaded(id)) {
            source.sendFailure(Component.literal("Unknown sound: " + soundId));
            return 0;
        }
        LOGGER.info("Playing: {} count={} start={}s duration={}s", id, count == 0 ? "infinite" : count, startSeconds, durationSeconds);
        api.play(id, count, startSeconds, durationSeconds);
        String msg = AudiolayerCommandSupport.playMessage(id, count, startSeconds, durationSeconds);
        source.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private CompletableFuture<Suggestions> suggestSoundIds(com.mojang.brigadier.context.CommandContext<?> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(api.listSounds().stream()
                .map(id -> net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()))
                .toList(), builder);
    }
}
