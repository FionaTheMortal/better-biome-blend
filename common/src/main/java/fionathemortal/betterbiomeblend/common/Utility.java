package fionathemortal.betterbiomeblend.common;

public class Utility
{
    public static int
    boolToInt(boolean value)
    {
        int result = value ? 1 : 0;

        return result;
    }

    public static int
    getLowerBits(int value, int bitCount)
    {
        int result = value & ((1 << bitCount) - 1);

        return result;
    }
}
