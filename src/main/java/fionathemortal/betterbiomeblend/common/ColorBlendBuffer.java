package fionathemortal.betterbiomeblend.common;

public final class ColorBlendBuffer
{
    public float[] color;

    public ColorBlendBuffer()
    {
        this.color = new float[3 * ColorBlending.BLEND_BUFFER_DIM * ColorBlending.BLEND_BUFFER_DIM * ColorBlending.BLEND_BUFFER_DIM];
    }
}
