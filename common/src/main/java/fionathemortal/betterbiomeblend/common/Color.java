package fionathemortal.betterbiomeblend.common;

public final class Color
{
    public static final float[] sRGBLUT = new float[256];

    static
    {
        for (int i = 0;
            i < 256;
            ++i)
        {
            float color = byteToNormalizedFloat(i);
            sRGBLUT[i] = sRGBToLinear(color);
        }
    }

    public static int
    makeRGBAWithFullAlpha(int R, int G, int B)
    {
        int result =
            ((0xFF & R) << 16) |
            ((0xFF & G) <<  8) |
            ((0xFF & B) <<  0) |
            ((0xFF    ) << 24);

        return result;
    }

    public static int
    RGBAGetR(int color)
    {
        int result = (color >> 16) & 0xFF;

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
        int result = color & 0xFF;

        return result;
    }

    public static float
    byteToNormalizedFloat(int color)
    {
        float result = (float)(0xFF & color) / 255.0f;

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
        float clamped = Math.max(Math.min(color, 1.0f), 0.0f);

        float result;

        if (clamped <= 0.0404482362771082f)
        {
            result = color / 12.92f;
        }
        else
        {
            result = (float)Math.pow((clamped + 0.055f) / 1.055f, 2.4f);
        }

        return result;
    }

    public static float
    linearTosRGB(float color)
    {
        float clamped = Math.max(Math.min(color, 1.0f), 0.0f);

        float result;

        if (clamped <= 0.00313066844250063f)
        {
            result = clamped * 12.92f;
        }
        else
        {
            result = 1.055f * (float)Math.pow(clamped, 1.0f / 2.4f) - 0.055f;
        }

        return result;
    }

    public static float
    sRGBByteToLinearFloat(int color)
    {
        float result = sRGBLUT[0xFF & color];

        return result;
    }

    public static byte
    linearFloatTosRGBByte(float color)
    {
        float sRGB = linearTosRGB(color);

        byte result = normalizedFloatToByte(sRGB);

        return result;
    }

    public static float
    sRGBByteToBlendSpace(int color)
    {
        return sRGBByteToLinearFloat(color);
    }

    public static byte
    blendSpaceTosRGBByte(float color)
    {
        return linearFloatTosRGBByte(color);
    }

    public static void
    sRGBByteToOKLabs(int rIn, int gIn, int bIn, float[] dest, int index)
    {
        float r = sRGBByteToLinearFloat(rIn);
        float g = sRGBByteToLinearFloat(gIn);
        float b = sRGBByteToLinearFloat(bIn);

        float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
        float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
        float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;

        float lRoot = (float)Math.cbrt(l);
        float mRoot = (float)Math.cbrt(m);
        float sRoot = (float)Math.cbrt(s);

        float LResult = 0.2104542553f*lRoot + 0.7936177850f*mRoot - 0.0040720468f*sRoot;
        float aResult = 1.9779984951f*lRoot - 2.4285922050f*mRoot + 0.4505937099f*sRoot;
        float bResult = 0.0259040371f*lRoot + 0.7827717662f*mRoot - 0.8086757660f*sRoot;

        dest[index + 0] = LResult;
        dest[index + 1] = aResult;
        dest[index + 2] = bResult;
    }

    public static void
    OKLabsTosRGBByte(float L, float a, float b, byte[] dest, int index)
    {
        float l_ = L + 0.3963377774f * a + 0.2158037573f * b;
        float m_ = L - 0.1055613458f * a - 0.0638541728f * b;
        float s_ = L - 0.0894841775f * a - 1.2914855480f * b;

        float l = l_*l_*l_;
        float m = m_*m_*m_;
        float s = s_*s_*s_;

        float rResult = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float gResult = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float bResult = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

        int rByte = linearFloatTosRGBByte(rResult);
        int gByte = linearFloatTosRGBByte(gResult);
        int bByte = linearFloatTosRGBByte(bResult);

        dest[index + 0] = (byte)rByte;
        dest[index + 1] = (byte)gByte;
        dest[index + 2] = (byte)bByte;
    }
}
