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

        float result = approxsRGBToLinear(colorF32);

        return result;
    }

    public static byte
    linearFloatTosRGBByte(float color)
    {
        float sRGB = approxLinearTosRGB(color);

        byte result = normalizedFloatToByte(sRGB);

        return result;
    }

    public static float
    approxExp2Polynomial(float x)
    {
        float x2 = x * x;

        float result =
            0x1.ffe7f78cbbe64p-1f +
            0x1.64c2647affd18p-1f * x +
            0x1.cb3820927bcf4p-3f * x2 +
            0x1.4362924454226p-4f * x2 * x;

        return result;
    }

    public static float
    approxExp2(float x)
    {
        int   xi = (int)Math.floor(x);
        float xf = x - xi;

        float result = (float)(1 << xi) * approxExp2Polynomial(xf);

        return result;
    }

    public static float
    approxLog2Polynomial(float x)
    {
        float x2 = x * x;

        float result =
            -0x1.914c3a9a6e978p+1f +
             0x1.81b8a29bb431cp+2f * x +
            -0x1.0804c14351802p+2f * x2 +
             0x1.3c0b018cf31f4p+0f * x2 * x;

        return result;
    }

    static final int getHighestSetBitIndexMasks[] = { 0x2, 0xC, 0xF0, 0xFF00, 0xFFFF0000 };
    static final int getHighestSetBitIndexShift[] = { 1, 2, 4, 8, 16 };

    public static int
    getHighestSetBitIndex(int x)
    {
        int result = 0;

        for (int i = 4;
            i >= 0;
            --i)
        {
            if ((x & getHighestSetBitIndexMasks[i]) != 0)
            {
                x >>>= getHighestSetBitIndexShift[i];
                result |= getHighestSetBitIndexShift[i];
            }
        }

        return result;
    }

    public static float
    approxLog2(float x)
    {
        int bits = Float.floatToIntBits(x);
        int exponent = (int)((bits >> 23) & ((1 << 23) - 1));
        int mantissa = (bits & ((1 << 23) - 1)) | (1 << 23);

        int msb = getHighestSetBitIndex(mantissa);
        int bias = msb + 1;

        int   resultExponent = exponent - (127 + 23) + bias;
        float resultMantissa = (float)(mantissa) / (float)(1 << bias);

        if (bits < 0)
        {
            resultMantissa = -resultMantissa;
        }

        float result = resultExponent + approxLog2Polynomial(resultMantissa);

        return result;
    }

    public static float
    approxPow(float base, float exponent)
    {
        float result = approxExp2(exponent * approxLog2(base));

        return result;
    }

    public static float
    approxsRGBToLinear(float color)
    {
        float result;

        if (color <= 0.0404482362771082f)
        {
            result = color / 12.92f;
        }
        else
        {
            result = approxPow((color + 0.055f) / 1.055f, 2.4f);
        }

        return result;
    }

    public static float
    approxLinearTosRGB(float color)
    {
        float result;

        if (color <= 0.00313066844250063f)
        {
            result = color * 12.92f;
        }
        else
        {
            result = 1.055f * approxPow(color, 1.0f / 2.4f) - 0.055f;
        }

        return result;
    }
}
