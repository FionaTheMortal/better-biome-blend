package fionathemortal.betterbiomeblend.common;

import net.minecraft.world.level.ColorResolver;

public final class LocalCache
{
    public ColorResolver lastColorResolver;
    public BlendChunk    lastBlendChunk;
    public int           lastColorType;

    public BlendChunk[]  blendChunks     = new BlendChunk[3];
    public int           blendChunkCount = 3;

    public static BlendChunk
    newBlendChunk()
    {
        BlendChunk result = new BlendChunk();

        result.acquire();

        return result;
    }

    public
    LocalCache()
    {
        for (int index = 0;
            index < blendChunkCount;
            ++index)
        {
            blendChunks[index] = newBlendChunk();
        }

        lastBlendChunk = blendChunks[0];
    }

    public void
    putChunk(BlendCache cache, BlendChunk chunk, int colorType, ColorResolver colorResolver)
    {
        BlendChunk prevChunk = this.blendChunks[colorType];

        this.lastColorResolver = colorResolver;
        this.lastBlendChunk    = chunk;
        this.lastColorType     = colorType;

        cache.releaseChunk(prevChunk);

        this.blendChunks[colorType] = chunk;
    }

    public void
    growBlendChunkArray(int colorType)
    {
        int oldBlendChunkCount = this.blendChunkCount;
        int newBlendChunkCount = colorType + 1;

        if (newBlendChunkCount > oldBlendChunkCount)
        {
            BlendChunk[] oldBlendChunks = this.blendChunks;
            BlendChunk[] newBlendChunks = new BlendChunk[colorType + 1];

            for (int index = 0;
                index < oldBlendChunkCount;
                ++index)
            {
                newBlendChunks[index] = oldBlendChunks[index];
            }

            for (int index = oldBlendChunkCount;
                index < newBlendChunkCount;
                ++index)
            {
                newBlendChunks[index] = newBlendChunk();
            }

            this.blendChunks     = newBlendChunks;
            this.blendChunkCount = newBlendChunkCount;
        }
    }
}
