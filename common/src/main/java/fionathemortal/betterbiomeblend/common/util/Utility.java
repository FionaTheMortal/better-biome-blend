package fionathemortal.betterbiomeblend.common.util;

public class Utility
{
    public static final int INVALID_CHUNK_KEY = -1;

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

    public static int
    boolToInt(boolean value)
    {
        int result = value ? 1 : 0;

        return result;
    }

    public static int
    lowerBitMask(int bitCount)
    {
        int result = (1 << bitCount) - 1;

        return result;
    }

    public static int
    lowerBits(int value, int bitCount)
    {
        int result = value & lowerBitMask(bitCount);

        return result;
    }

    public static int
    blockToChunk(int position)
    {
        int result = position >> 4;

        return result;
    }
}
