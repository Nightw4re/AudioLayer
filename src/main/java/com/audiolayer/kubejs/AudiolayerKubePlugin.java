package com.audiolayer.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * Registers Audiolayer bindings with KubeJS.
 *
 * <p>Discovered automatically via {@code META-INF/services/dev.latvian.mods.kubejs.plugin.KubeJSPlugin}.
 *
 * <p>After registration, scripts can call:
 * <pre>
 *   Audiolayer.play("audiolayer:music.theme");
 *   Audiolayer.play("audiolayer:music.theme", 3, 0, 0);
 *   Audiolayer.stop();
 *   Audiolayer.isLoaded("audiolayer:music.theme");
 *   Audiolayer.listSounds();
 *   Audiolayer.reload();
 * </pre>
 */
public class AudiolayerKubePlugin implements KubeJSPlugin {

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Audiolayer", new AudiolayerJSWrapper());
    }
}
