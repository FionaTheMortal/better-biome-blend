package fionathemortal.betterbiomeblend.common;

public final class ColorCaching
{
    public static final int INVALID_CHUNK_KEY = -1;

    public static int
    getArrayIndex(int dim, int x, int y, int z)
    {
        int result = x + y * dim + z * dim * dim;

        return result;
    }

    public static long
    getChunkKey(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long result =
            ((long)(chunkX & 0x03FFFFFF)      ) |
            ((long)(chunkZ & 0x03FFFFFF) << 26) |
            ((long)(chunkY & 0x1F      ) << 52) |
            ((long)(colorType          ) << 57);

        return result;
    }
}
