package fionathemortal.betterbiomeblend;

import java.lang.reflect.Field;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;

public final class OptifineCompatibility {

    private static Class<?> ChunkCacheOFClass;
    private static Field ChunkCacheOFChunkCacheField;
    private static boolean successfullyInitialized;
    static {
        if (isOptifinePresent()) {
            try {
                ChunkCacheOFClass = Class.forName("net.optifine.override.ChunkCacheOF");
                ChunkCacheOFChunkCacheField = ChunkCacheOFClass.getDeclaredField("chunkCache");
                ChunkCacheOFChunkCacheField.setAccessible(true);

                successfullyInitialized = true;
            } catch (Throwable ignored) {}
        }
    }

    public static boolean isChunkCacheOF(IBlockAccess blockAccess) {
        boolean result = false;

        if (successfullyInitialized) {
            result = ChunkCacheOFClass.isInstance(blockAccess);
        }

        return result;
    }

    public static ChunkCache getChunkCacheFromChunkCacheOF(IBlockAccess blockAccess) {
        ChunkCache result = null;

        try {
            result = (ChunkCache) ChunkCacheOFChunkCacheField.get(blockAccess);
        } catch (IllegalAccessException ignored) {}

        return result;
    }

    public static boolean isOptifinePresent() {
        Class<?> config = null;

        try {
            config = Class.forName("net.optifine.Config");
        } catch (ClassNotFoundException failed) {
            try {
                config = Class.forName("Config");
            } catch (ClassNotFoundException ignored) {}
        }

        boolean result = config != null;

        return result;
    }
}
