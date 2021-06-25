package fionathemortal.betterbiomeblend;

public interface ColorChunkCacheProvider
{
    ColorChunkCache getColorChunkCache();

    ThreadLocal<ColorChunk> getTreadLocalGrassChunk();

    ThreadLocal<ColorChunk> getTreadLocalWaterChunk();

    ThreadLocal<ColorChunk> getTreadLocalFoliageChunk();
}
