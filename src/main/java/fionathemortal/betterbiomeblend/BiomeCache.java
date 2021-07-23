package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class BiomeCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> hash;

    public
    BiomeCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);

        for (int index = 0;
            index < count;
            ++index)
        {
            BiomeChunk chunk = new BiomeChunk();

            chunk.acquire();
            chunk.key -= index;

            hash.put(chunk.key, chunk);
        }
    }

    public void
    releaseChunk(BiomeChunk chunk)
    {
        int refCount = chunk.release();
    }

    public BiomeChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkZ)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, 0);

        lock.lock();

        BiomeChunk result = hash.getAndMoveToFirst(key);

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

            Arrays.fill(result.data, null);

            hash.putAndMoveToFirst(result.key, result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
