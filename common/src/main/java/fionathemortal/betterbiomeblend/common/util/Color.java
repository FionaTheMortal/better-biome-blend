package fionathemortal.betterbiomeblend.common.util;

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
            ((0xFF & B)      ) |
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

    /* NOTE: Copied from stb_image_resive.h and modified */

    static int[] fp32_to_srgb8_tab4 = {
        0x0073000d, 0x007a000d, 0x0080000d, 0x0087000d, 0x008d000d, 0x0094000d, 0x009a000d, 0x00a1000d,
        0x00a7001a, 0x00b4001a, 0x00c1001a, 0x00ce001a, 0x00da001a, 0x00e7001a, 0x00f4001a, 0x0101001a,
        0x010e0033, 0x01280033, 0x01410033, 0x015b0033, 0x01750033, 0x018f0033, 0x01a80033, 0x01c20033,
        0x01dc0067, 0x020f0067, 0x02430067, 0x02760067, 0x02aa0067, 0x02dd0067, 0x03110067, 0x03440067,
        0x037800ce, 0x03df00ce, 0x044600ce, 0x04ad00ce, 0x051400ce, 0x057b00c5, 0x05dd00bc, 0x063b00b5,
        0x06970158, 0x07420142, 0x07e30130, 0x087b0120, 0x090b0112, 0x09940106, 0x0a1700fc, 0x0a9500f2,
        0x0b0f01cb, 0x0bf401ae, 0x0ccb0195, 0x0d950180, 0x0e56016e, 0x0f0d015e, 0x0fbc0150, 0x10630143,
        0x11070264, 0x1238023e, 0x1357021d, 0x14660201, 0x156601e9, 0x165a01d3, 0x174401c0, 0x182401af,
        0x18fe0331, 0x1a9602fe, 0x1c1502d2, 0x1d7e02ad, 0x1ed4028d, 0x201a0270, 0x21520256, 0x227d0240,
        0x239f0443, 0x25c003fe, 0x27bf03c4, 0x29a10392, 0x2b6a0367, 0x2d1d0341, 0x2ebe031f, 0x304d0300,
        0x31d105b0, 0x34a80555, 0x37520507, 0x39d504c5, 0x3c37048b, 0x3e7c0458, 0x40a8042a, 0x42bd0401,
        0x44c20798, 0x488e071e, 0x4c1c06b6, 0x4f76065d, 0x52a50610, 0x55ac05cc, 0x5892058f, 0x5b590559,
        0x5e0c0a23, 0x631c0980, 0x67db08f6, 0x6c55087f, 0x70940818, 0x74a007bd, 0x787d076c, 0x7c330723
    };

    public static byte
    linearFloatTosRGBByte(float in)
    {
        final int almostone = 0x3f7fffff;
        final int minval    = (127-13) << 23;

        final float almostoneF = Float.intBitsToFloat(almostone);
        final float minvalF    = Float.intBitsToFloat(minval);

        /* NOTE: Bounds checking on integers. I do not care about NaNs and
         *       comparisons are generally faster on integers. I think this gives
         *       the same result as the float comparisons. TODO: Check! */
        /*
        int inBits = Math.max(Math.min(Float.floatToIntBits(in), almostone), minval);
        */

        if (!(in > minvalF)) // written this way to catch NaNs
            in = minvalF;
        if (in > almostoneF)
            in = almostoneF;

        int inBits = Float.floatToIntBits(in);

        int tab   = fp32_to_srgb8_tab4[(inBits - minval) >>> 20];
        int bias  = (tab >>> 16) << 9;
        int scale = tab & 0xffff;

        int t = (inBits >>> 12) & 0xff;

        return (byte) ((bias + scale * t) >>> 16);
    }

    public static void
    sRGBByteToOKLabs(int color, float[] dest, int index)
    {
        int rIn = RGBAGetR(color);
        int gIn = RGBAGetG(color);
        int bIn = RGBAGetB(color);

        float r = sRGBByteToLinearFloat(rIn);
        float g = sRGBByteToLinearFloat(gIn);
        float b = sRGBByteToLinearFloat(bIn);

        float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
        float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
        float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;

        float lRoot = (float)Math.cbrt(l);
        float mRoot = (float)Math.cbrt(m);
        float sRoot = (float)Math.cbrt(s);

        float LResult = 0.2104542553f * lRoot + 0.7936177850f * mRoot - 0.0040720468f * sRoot;
        float aResult = 1.9779984951f * lRoot - 2.4285922050f * mRoot + 0.4505937099f * sRoot;
        float bResult = 0.0259040371f * lRoot + 0.7827717662f * mRoot - 0.8086757660f * sRoot;

        dest[index    ] = LResult;
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

        float rResult =  4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float gResult = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float bResult = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

        int rByte = linearFloatTosRGBByte(rResult);
        int gByte = linearFloatTosRGBByte(gResult);
        int bByte = linearFloatTosRGBByte(bResult);

        dest[index    ] = (byte)rByte;
        dest[index + 1] = (byte)gByte;
        dest[index + 2] = (byte)bByte;
    }

    public static void
    OKLabsTosRGBAInt(float L, float a, float b, int[] dest, int index)
    {
        float l_ = L + 0.3963377774f * a + 0.2158037573f * b;
        float m_ = L - 0.1055613458f * a - 0.0638541728f * b;
        float s_ = L - 0.0894841775f * a - 1.2914855480f * b;

        float l = l_ * l_ * l_;
        float m = m_ * m_ * m_;
        float s = s_ * s_ * s_;

        float rResult =  4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float gResult = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float bResult = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

        int rByte = linearFloatTosRGBByte(rResult);
        int gByte = linearFloatTosRGBByte(gResult);
        int bByte = linearFloatTosRGBByte(bResult);

        int color = makeRGBAWithFullAlpha(rByte, gByte, bByte);

        dest[index] = color;
    }
}
