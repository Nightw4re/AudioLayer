package com.audiolayer.commands;

import com.audiolayer.api.SoundId;
import com.audiolayer.network.AudiolayerPlayPacket;
import com.audiolayer.network.AudiolayerStopPacket;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class AudiolayerServerCommands {
    private final AudiolayerManager manager;

    public AudiolayerServerCommands(AudiolayerManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("audiolayer")
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    String sounds = manager.listSounds().stream()
                                            .map(SoundId::toString)
                                            .collect(Collectors.joining(", "));
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            sounds.isBlank() ? "No sounds loaded" : "Loaded sounds: " + sounds
                                    ), false);
                                    return 1;
                                }))
                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(context -> {
                                    var summary = manager.reload();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            "Reloaded: " + summary.loadedAssets() + " sounds (" + summary.reusedAssets() + " cached)"
                                    ), true);
                                    return 1;
                                }))
                        .then(Commands.literal("stop")
                                .executes(context -> {
                                    PacketDistributor.sendToPlayer(context.getSource().getPlayerOrException(),
                                            new AudiolayerStopPacket());
                                    context.getSource().sendSuccess(() -> Component.literal("Playback stopped"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("play")
                                .then(Commands.argument("sound_id", ResourceLocationArgument.id())
                                        .suggests(this::suggestSoundIds)
                                        .executes(context -> sendPlay(context.getSource(),
                                                ResourceLocationArgument.getId(context, "sound_id").toString(), 1, 0f, 0f))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> sendPlay(context.getSource(),
                                                        ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                        IntegerArgumentType.getInteger(context, "count"), 0f, 0f))
                                                .then(Commands.argument("start", FloatArgumentType.floatArg(0f))
                                                        .executes(context -> sendPlay(context.getSource(),
                                                                ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                FloatArgumentType.getFloat(context, "start"), 0f))
                                                        .then(Commands.argument("duration", FloatArgumentType.floatArg(0f))
                                                                .executes(context -> sendPlay(context.getSource(),
                                                                        ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                        FloatArgumentType.getFloat(context, "start"),
                                                                        FloatArgumentType.getFloat(context, "duration"))))))))
        );
    }

    private int sendPlay(net.minecraft.commands.CommandSourceStack source, String soundId, int count, float start, float duration) {
        SoundId id = AudiolayerCommandSupport.parseSoundId(soundId);
        if (!manager.isLoaded(id)) {
            source.sendFailure(Component.literal("Unknown sound: " + soundId));
            return 0;
        }
        try {
            PacketDistributor.sendToPlayer(source.getPlayerOrException(),
                    new AudiolayerPlayPacket(id, count, start, duration));
            String msg = AudiolayerCommandSupport.playMessage(id, count, start, duration);
            source.sendSuccess(() -> Component.literal(msg), false);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Could not send to player: " + e.getMessage()));
        }
        return 1;
    }

    private CompletableFuture<Suggestions> suggestSoundIds(com.mojang.brigadier.context.CommandContext<?> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(manager.listSounds().stream()
                .map(id -> net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()))
                .toList(), builder);
    }
}
