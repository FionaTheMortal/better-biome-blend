package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public final class ColorCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<ColorChunk> hash;

    public
    ColorCache(int count)
    {
        lock      = new ReentrantLock();
        hash      = new Long2ObjectLinkedOpenHashMap<ColorChunk>(count);

        for (int index = 0;
            index < count;
            ++index)
        {
            ColorChunk chunk = new ColorChunk();

            chunk.acquire();
            chunk.key -= index;

            hash.put(chunk.key, chunk);
        }
    }

    public void
    releaseChunk(ColorChunk chunk)
    {
        int refCount = chunk.release();
    }

    public ColorChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, colorType);
        lock.lock();

        ColorChunk result = hash.getAndMoveToFirst(key);

        if (result == null)
        {
            for (;;)
            {
                long lastKey = hash.lastLongKey();

                result = hash.removeLast();

                if (result.getReferenceCount() == 1)
                {
                    break;
                }
                else
                {
                    hash.putAndMoveToFirst(lastKey, result);
                }
            }

            result.key = key;

            Arrays.fill(result.data, (byte)-1);

            hash.putAndMoveToFirst(result.key, result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
