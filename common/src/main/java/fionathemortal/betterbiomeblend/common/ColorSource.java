package fionathemortal.betterbiomeblend.common;

import fionathemortal.betterbiomeblend.common.cache.Slice;
import fionathemortal.betterbiomeblend.common.cache.SliceCache;
import fionathemortal.betterbiomeblend.common.util.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;

public final class ColorSource
{
    private final ColorConfig config;
    private final ClientLevel parent;

    private final SliceCache resultCache;
    private final SliceCache sourceCache;

    public
    ColorSource(ColorConfig config, ClientLevel parent)
    {
        this.config = config;
        this.parent = parent;

        resultCache = new SliceCache(config, config.getResultConfig(), 512, 1024);
        sourceCache = new SliceCache(config, config.getSourceConfig(), 512, 0);
    }

    public Slice
    getSlice(int sliceX, int sliceY, int sliceZ, int colorType)
    {
        return resultCache.getSlice(sliceX, sliceY, sliceZ, colorType, false, false);
    }

    public Slice
    genSlice(int chunkX, int chunkY, int chunkZ, int colorType, ColorResolver colorResolver)
    {
        long ticket = resultCache.getGenTick();

        ColorGenContext context = ColorGen.gatherColors(
            parent,
            colorResolver,
            colorType,
            config,
            sourceCache,
            chunkX,
            chunkY,
            chunkZ);

        boolean isSolid = context.isSolid();

        Slice result = resultCache.newSlice(chunkX, chunkY, chunkZ, colorType, isSolid);

        if (isSolid)
        {
            result.solidColor = context.getSingleColor();
        }
        else
        {
            ColorGen.blendColors(
                context,
                result.blockColors,
                0,
                0,
                0,
                Util.CHUNK_SIZE,
                Util.CHUNK_SIZE);
        }

        result = resultCache.putSlice(result, ticket);

        ColorGen.endChunkGen(context);

        return result;
    }

    public void
    releaseSlice(Slice slice)
    {
        if (slice != null && resultCache.ownsSlice(slice))
        {
            resultCache.releaseSlice(slice);
        }
    }

    public void
    invalidateChunk(int chunkX, int chunkZ)
    {
        sourceCache.invalidateChunk(chunkX, chunkZ, true, false);
        resultCache.invalidateChunk(chunkX, chunkZ, true, true);
    }

    public void
    destroy()
    {
        sourceCache.destroy();
        resultCache.destroy();
    }
}
