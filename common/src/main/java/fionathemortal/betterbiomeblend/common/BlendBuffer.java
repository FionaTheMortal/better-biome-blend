package fionathemortal.betterbiomeblend.common;

public final class BlendBuffer
{
    public float[] colors;

    public int blendRadius;

    public BlendBuffer(int blendRadius)
    {
        final int size = BlendConfig.getBlendBufferSize(blendRadius);

        this.blendRadius = blendRadius;
        this.colors      = new float[3 * size * size * size];
    }
}
