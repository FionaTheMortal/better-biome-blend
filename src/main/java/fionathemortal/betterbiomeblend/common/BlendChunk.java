package fionathemortal.betterbiomeblend.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class BlendChunk
{
    public byte[] data;
    public long   key;
    public int    invalidationCounter;

    public AtomicInteger refCount = new AtomicInteger();

    public
    BlendChunk()
    {
        this.data = new byte[5 * 5 * 5 * 3];

        this.markAsInvalid();
    }

    public int
    getReferenceCount()
    {
        int result = refCount.get();

        return result;
    }

    public int
    release()
    {
        int result = refCount.decrementAndGet();

        return result;
    }

    public void
    acquire()
    {
        refCount.incrementAndGet();
    }

    public void
    markAsInvalid()
    {
        key = ColorCaching.INVALID_CHUNK_KEY;
    }

    public int
    getColor(int x, int y, int z)
    {
        int blockX = (x & 15);
        int blockY = (y & 15);
        int blockZ = (z & 15);

        int sectionX = blockX >> 2;
        int sectionY = blockY >> 2;
        int sectionZ = blockZ >> 2;

        int offsetX = blockX & 3;
        int offsetY = blockY & 3;
        int offsetZ = blockZ & 3;

        int weight0 = (4 - offsetX) * (4 - offsetZ) * (4 - offsetY);
        int weight1 = (    offsetX) * (4 - offsetZ) * (4 - offsetY);
        int weight2 = (4 - offsetX) * (    offsetZ) * (4 - offsetY);
        int weight3 = (    offsetX) * (    offsetZ) * (4 - offsetY);
        int weight4 = (4 - offsetX) * (4 - offsetZ) * (    offsetY);
        int weight5 = (    offsetX) * (4 - offsetZ) * (    offsetY);
        int weight6 = (4 - offsetX) * (    offsetZ) * (    offsetY);
        int weight7 = (    offsetX) * (    offsetZ) * (    offsetY);

        int index0 = 3 * ColorBlending.getCacheArrayIndex(5, sectionX, sectionY, sectionZ);
        int index1 = index0 +  3;
        int index2 = index0 + 15;
        int index3 = index0 + 18;
        int index4 = index0 + 75;
        int index5 = index0 + 78;
        int index6 = index0 + 90;
        int index7 = index0 + 93;

        long packed0 = ((long)(0xFF & data[index0 + 0])) | ((long)(0xFF & data[index0 + 1]) << 24) | ((long)(0xFF & data[index0 + 2]) << 48);
        long packed1 = ((long)(0xFF & data[index1 + 0])) | ((long)(0xFF & data[index1 + 1]) << 24) | ((long)(0xFF & data[index1 + 2]) << 48);
        long packed2 = ((long)(0xFF & data[index2 + 0])) | ((long)(0xFF & data[index2 + 1]) << 24) | ((long)(0xFF & data[index2 + 2]) << 48);
        long packed3 = ((long)(0xFF & data[index3 + 0])) | ((long)(0xFF & data[index3 + 1]) << 24) | ((long)(0xFF & data[index3 + 2]) << 48);
        long packed4 = ((long)(0xFF & data[index4 + 0])) | ((long)(0xFF & data[index4 + 1]) << 24) | ((long)(0xFF & data[index4 + 2]) << 48);
        long packed5 = ((long)(0xFF & data[index5 + 0])) | ((long)(0xFF & data[index5 + 1]) << 24) | ((long)(0xFF & data[index5 + 2]) << 48);
        long packed6 = ((long)(0xFF & data[index6 + 0])) | ((long)(0xFF & data[index6 + 1]) << 24) | ((long)(0xFF & data[index6 + 2]) << 48);
        long packed7 = ((long)(0xFF & data[index7 + 0])) | ((long)(0xFF & data[index7 + 1]) << 24) | ((long)(0xFF & data[index7 + 2]) << 48);

        long v0 = packed0 * weight0;
        long v1 = packed1 * weight1;
        long v2 = packed2 * weight2;
        long v3 = packed3 * weight3;
        long v4 = packed4 * weight4;
        long v5 = packed5 * weight5;
        long v6 = packed6 * weight6;
        long v7 = packed7 * weight7;

        long packedResult = (v0 + v1 + v2 + v3 + v4 + v5 + v6 + v7) / 64;

        int r = (int)(packedResult      ) & 0xFF;
        int g = (int)(packedResult >> 24) & 0xFF;
        int b = (int)(packedResult >> 48) & 0xFF;

        int result = Color.makeRGBAWithFullAlpha(r, g, b);

        return result;
    }
}
