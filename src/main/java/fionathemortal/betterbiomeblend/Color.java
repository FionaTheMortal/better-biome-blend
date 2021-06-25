package fionathemortal.betterbiomeblend;

public final class Color
{
    public static int
    makeRGBAWithFullAlpha(int R, int G, int B)
    {
        int result =
            ((0xFF & R) <<  0) |
            ((0xFF & G) <<  8) |
            ((0xFF & B) << 16) |
            0xFF000000;

        return result;
    }

    public static int
    RGBAGetR(int color)
    {
        int result = color & 0xFF;

        return result;
    }

    public static int
    RGBAGetG(int color)
    {
        int result = (color >> 8) & 0xFF;

        return result;
    }

    public static int
    RGBAGetB(int color)
    {
        int result = (color >> 16) & 0xFF;

        return result;
    }
}
