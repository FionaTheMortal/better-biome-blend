package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.ColorChunk;
import fionathemortal.betterbiomeblend.ColorChunkCache;
import fionathemortal.betterbiomeblend.ColorChunkCacheProvider;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements ColorChunkCacheProvider
{
    public final ThreadLocal<ColorChunk> threadLocalGrassChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    public final ThreadLocal<ColorChunk> threadLocalWaterChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    public final ThreadLocal<ColorChunk> threadLocalFoliageChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    public final ColorChunkCache colorChunkCache = new ColorChunkCache(2048);

    @Override
    public ColorChunkCache
    getColorChunkCache()
    {
        return colorChunkCache;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalGrassChunk()
    {
        return threadLocalGrassChunk;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalWaterChunk()
    {
        return threadLocalWaterChunk;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalFoliageChunk()
    {
        return threadLocalFoliageChunk;
    }
}
