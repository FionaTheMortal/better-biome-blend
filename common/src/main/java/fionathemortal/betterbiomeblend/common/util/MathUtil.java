package fionathemortal.betterbiomeblend.common.util;

public class MathUtil
{
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
}
