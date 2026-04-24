package com.audiolayer.commands;

import com.audiolayer.api.SoundId;
import com.audiolayer.network.AudiolayerPlayPacket;
import com.audiolayer.network.AudiolayerStopPacket;
import com.audiolayer.registry.AudiolayerManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Files;
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
                                })
                                .then(Commands.argument("category", StringArgumentType.word())
                                        .executes(context -> {
                                            String category = StringArgumentType.getString(context, "category");
                                            PacketDistributor.sendToPlayer(context.getSource().getPlayerOrException(),
                                                    new AudiolayerStopPacket(category));
                                            context.getSource().sendSuccess(() -> Component.literal("Playback stopped: " + category), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("sound_id", StringArgumentType.greedyString())
                                        .suggests(this::suggestSoundIds)
                                        .executes(context -> {
                                            String raw = StringArgumentType.getString(context, "sound_id");
                                            SoundId id = AudiolayerCommandSupport.parseSoundId(raw);
                                            return manager.get(id).map(asset -> {
                                                boolean sourceExists = Files.exists(asset.sourceFile());
                                                context.getSource().sendSuccess(() -> Component.literal(
                                                        "id=" + asset.soundId()
                                                                + " | source=" + (sourceExists ? "ok" : "missing")
                                                                + " | duration=" + asset.durationSeconds() + "s"
                                                                + " | path=" + asset.sourceFile()
                                                ), false);
                                                return 1;
                                            }).orElseGet(() -> {
                                                context.getSource().sendFailure(Component.literal("Unknown sound: " + raw));
                                                return 0;
                                            });
                                        })))
                        .then(Commands.literal("test")
                                .then(Commands.literal("setup")
                                        .executes(context -> {
                                            var player = context.getSource().getPlayerOrException();
                                            player.addItem(new ItemStack(Items.JUKEBOX));

                                            var etchedBlankDisc = BuiltInRegistries.ITEM.getOptional(
                                                    ResourceLocation.parse("etched:blank_music_disc")).orElse(null);
                                            var etchedMusicLabel = BuiltInRegistries.ITEM.getOptional(
                                                    ResourceLocation.parse("etched:music_label")).orElse(null);

                                            if (etchedBlankDisc != null && etchedMusicLabel != null) {
                                                player.addItem(new ItemStack(etchedBlankDisc));
                                                player.addItem(new ItemStack(etchedMusicLabel));

                                                var etchedDisc = BuiltInRegistries.ITEM.getOptional(
                                                        ResourceLocation.parse("etched:etched_music_disc")).orElse(null);
                                                var musicComponent = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(
                                                        ResourceLocation.parse("etched:music")).orElse(null);
                                                if (etchedDisc != null && musicComponent != null) {
                                                    manager.listSounds().stream().limit(3).forEach(id -> {
                                                        var stack = new ItemStack(etchedDisc);
                                                        stack.set(DataComponents.CUSTOM_NAME, Component.literal(id.path()));
                                                        player.addItem(stack);
                                                    });
                                                }
                                                context.getSource().sendSuccess(() -> Component.literal(
                                                        "Given: jukebox, blank_music_disc, music_label" +
                                                        (manager.listSounds().isEmpty() ? "" :
                                                        " + " + Math.min(3, manager.listSounds().size()) + " pre-named etched disc(s). " +
                                                        "Use Etching Table to etch the blank disc with sound: " +
                                                        manager.listSounds().stream().findFirst().map(SoundId::toString).orElse(""))
                                                ), false);
                                            } else {
                                                context.getSource().sendSuccess(() -> Component.literal(
                                                        "Given: jukebox. Etched not loaded — use /audiolayer play <sound_id> to test directly."
                                                ), false);
                                            }
                                            return 1;
                                        })))
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
                                                                        FloatArgumentType.getFloat(context, "duration")))
                                                                .then(Commands.argument("volume", FloatArgumentType.floatArg(0f))
                                                                        .executes(context -> sendPlay(context.getSource(),
                                                                                ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                                FloatArgumentType.getFloat(context, "start"),
                                                                                FloatArgumentType.getFloat(context, "duration"),
                                                                                FloatArgumentType.getFloat(context, "volume"),
                                                                                1f,
                                                                                "master"))
                                                                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(0.01f))
                                                                                .executes(context -> sendPlay(context.getSource(),
                                                                                        ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                                        FloatArgumentType.getFloat(context, "start"),
                                                                                        FloatArgumentType.getFloat(context, "duration"),
                                                                                        FloatArgumentType.getFloat(context, "volume"),
                                                                                        FloatArgumentType.getFloat(context, "pitch"),
                                                                                        "master"))
                                                                                .then(Commands.argument("category", StringArgumentType.word())
                                                                                        .executes(context -> sendPlay(context.getSource(),
                                                                                                ResourceLocationArgument.getId(context, "sound_id").toString(),
                                                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                                                FloatArgumentType.getFloat(context, "start"),
                                                                                                FloatArgumentType.getFloat(context, "duration"),
                                                                                                FloatArgumentType.getFloat(context, "volume"),
                                                                                                FloatArgumentType.getFloat(context, "pitch"),
                                                                                                StringArgumentType.getString(context, "category")))))))))))
        );
    }

    private int sendPlay(net.minecraft.commands.CommandSourceStack source, String soundId, int count, float start, float duration) {
        return sendPlay(source, soundId, count, start, duration, 1f, 1f, "master");
    }

    private int sendPlay(
            net.minecraft.commands.CommandSourceStack source,
            String soundId,
            int count,
            float start,
            float duration,
            float volume,
            float pitch,
            String category
    ) {
        SoundId id = AudiolayerCommandSupport.parseSoundId(soundId);
        if (!manager.isLoaded(id)) {
            source.sendFailure(Component.literal("Unknown sound: " + soundId));
            return 0;
        }
        try {
            PacketDistributor.sendToPlayer(source.getPlayerOrException(),
                    new AudiolayerPlayPacket(id, count, start, duration, volume, pitch, category));
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
