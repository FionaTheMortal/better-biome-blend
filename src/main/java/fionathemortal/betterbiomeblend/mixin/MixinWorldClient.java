package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.ColorChunk;
import fionathemortal.betterbiomeblend.ColorChunkCache;
import fionathemortal.betterbiomeblend.ColorChunkCacheProvider;
import net.minecraft.client.multiplayer.WorldClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient implements ColorChunkCacheProvider
{
    @Unique
    public final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalGrassChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    public final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalWaterChunk   =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    public final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalFoliageChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    public final ThreadLocal<ColorChunk> betterBiomeBlend$threadLocalGenericChunk =
        ThreadLocal.withInitial(
            () ->
            {
                ColorChunk chunk = new ColorChunk();
                chunk.acquire();
                return chunk;
            });

    @Unique
    public final ColorChunkCache betterBiomeBlend$colorChunkCache = new ColorChunkCache(2048);

    @Override
    public ColorChunkCache
    getColorChunkCache()
    {
        return betterBiomeBlend$colorChunkCache;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalGrassChunk()
    {
        return betterBiomeBlend$threadLocalGrassChunk;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalWaterChunk()
    {
        return betterBiomeBlend$threadLocalWaterChunk;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalFoliageChunk()
    {
        return betterBiomeBlend$threadLocalFoliageChunk;
    }

    @Override
    public ThreadLocal<ColorChunk>
    getTreadLocalGenericChunk()
    {
        return betterBiomeBlend$threadLocalGenericChunk;
    }
}