package fionathemortal.betterbiomeblend.common.util;

public class Util
{
    public static final boolean RUNTIME_FALSE;
    public static final boolean RUNTIME_TRUE;

    static
    {
        RUNTIME_FALSE = false;
        RUNTIME_TRUE  = true;
    }

    public static final int CHUNK_SIZE_LOG_2 = 4;
    public static final int CHUNK_SIZE       = (1 << CHUNK_SIZE_LOG_2);

    public static int
    lowerBitMask32(int bitCount)
    {
        return (1 << bitCount) - 1;
    }

    public static long
    lowerBitMask64(int bitCount)
    {
        return ((long)1 << bitCount) - 1;
    }

    public static int
    lowerBits(int value, int bitCount)
    {
        return value & lowerBitMask32(bitCount);
    }

    public static long
    lowerBits(long value, int bitCount)
    {
        long mask = lowerBitMask64(bitCount);
        long result = value & mask;

        return result;
    }

    public static int
    blockToChunk(int position)
    {
        return position >> CHUNK_SIZE_LOG_2;
    }

    public static int
    chunkToBlock(int chunk)
    {
        return chunk << CHUNK_SIZE_LOG_2;
    }

    public static int
    getBlockInChunk(int block)
    {
        return block & (CHUNK_SIZE - 1);
    }
}
