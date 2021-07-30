package fionathemortal.betterbiomeblend;

public final class ColorBlendBuffer
{
    public int blendRadius;

    public byte[] color;

    public float[] R;
    public float[] G;
    public float[] B;

    public ColorBlendBuffer(int blendRadius)
    {
        int genCacheDim = 16 + 2 * blendRadius;

        this.blendRadius = blendRadius;

        this.color = new byte[3 * genCacheDim * genCacheDim];

        this.R = new float[genCacheDim];
        this.G = new float[genCacheDim];
        this.B = new float[genCacheDim];
    }
}
