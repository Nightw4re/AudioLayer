package com.audiolayer.testsupport;

public final class TestRunner {
    private static final String[] TESTS = {
            "com.audiolayer.api.SoundIdTest",
            "com.audiolayer.api.AudiolayerProviderTest",
            "com.audiolayer.config.FilenameSanitizerTest",
            "com.audiolayer.config.SoundIdMapperTest",
            "com.audiolayer.audio.HashServiceTest",
            "com.audiolayer.audio.InputAudioScannerTest",
            "com.audiolayer.audio.StreamLoopControllerTest",
            "com.audiolayer.cache.JsonCacheIndexRepositoryTest",
            "com.audiolayer.commands.AudiolayerCommandSupportTest",
            "com.audiolayer.commands.AudiolayerCommandReloadTest",
            "com.audiolayer.commands.AudiolayerCommandDebugTest",
            "com.audiolayer.commands.AudiolayerCommandPlayTest",
            "com.audiolayer.registry.AudiolayerManagerConversionTest",
            "com.audiolayer.registry.AudioRegistryServiceTest",
            "com.audiolayer.registry.AudiolayerManagerTest",
            "com.audiolayer.registry.AudiolayerManagerCacheTest",
            "com.audiolayer.resource.RuntimeResourcePackWriterTest"
    };

    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (String test : TESTS) {
            Class<?> clazz = Class.forName(test);
            clazz.getMethod("run").invoke(null);
            passed++;
            System.out.println("PASS " + test);
        }
        System.out.println("Executed " + passed + " tests");
    }
}
