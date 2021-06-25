package fionathemortal.betterbiomeblend;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.swing.plaf.basic.BasicTreeUI;
import java.lang.reflect.Field;

public final class OptifineCompatibility
{
    private static Class<?> ChunkCacheOFClass;
    private static Field    ChunkCacheOFChunkCacheField;
    private static boolean  successfullyInitialized;
    static
    {
        if (isOptifinePresent())
        {
            try
            {
                ChunkCacheOFClass = Class.forName("net.optifine.override.ChunkCacheOF");
                ChunkCacheOFChunkCacheField = ObfuscationReflectionHelper.findField(ChunkCacheOFClass, "chunkCache");

                successfullyInitialized = true;
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    public static boolean
    isChunkCacheOF(IBlockAccess blockAccess)
    {
        boolean result = false;

        if (successfullyInitialized)
        {
            result = ChunkCacheOFClass.isInstance(blockAccess);
        }

        return result;
    }

    public static ChunkCache
    getChunkCacheFromChunkCacheOF(IBlockAccess blockAccess)
    {
        ChunkCache result = null;

        try
        {
            result = (ChunkCache)ChunkCacheOFChunkCacheField.get(blockAccess);
        }
        catch (IllegalAccessException ignored)
        {
        }

        return result;
    }

    public static boolean
    isOptifinePresent()
    {
        Class<?> config = null;

        try
        {
            config = Class.forName("net.optifine.Config");
        }
        catch (ClassNotFoundException failed)
        {
            try
            {
                config = Class.forName("Config");
            }
            catch (ClassNotFoundException ignored)
            {
            }
        }

        boolean result = config != null;

        return result;
    }
}
