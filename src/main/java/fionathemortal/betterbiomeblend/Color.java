package fionathemortal.betterbiomeblend;

public final class Color
{
    public static int
    makeRGBAWithFullAlpha(int R, int G, int B)
    {
        int result =
            ((0xFF & R) << 0)  |
            ((0xFF & G) << 8)  |
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

    public static float
    byteToNormalizedFloat(int color)
    {
        float result = (float)color / 255.0f;

        return result;
    }

    public static byte
    normalizedFloatToByte(float color)
    {
        byte result = (byte)Math.round(color * 255.0f);

        return result;
    }

    public static float
    sRGBToLinear(float color)
    {
        float result;

        if (color <= 0.0404482362771082f)
        {
            result = color / 12.92f;
        }
        else
        {
            result = (float)Math.pow((color + 0.055f) / 1.055f, 2.4f);
        }

        return result;
    }

    public static float
    linearTosRGB(float color)
    {
        float result;

        if (color <= 0.00313066844250063f)
        {
            result = color * 12.92f;
        }
        else
        {
            result = 1.055f * (float)Math.pow(color, 1.0f / 2.4f) - 0.055f;
        }

        return result;
    }

    public static float
    sRGBByteToLinearFloat(int color)
    {
        float colorF32 = byteToNormalizedFloat(color);

        float result = sRGBToLinear(colorF32);

        return result;
    }

    public static byte
    linearFloatTosRGBByte(float color)
    {
        float sRGB = linearTosRGB(color);

        byte result = normalizedFloatToByte(sRGB);

        return result;
    }
}
