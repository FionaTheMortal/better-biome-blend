package fionathemortal.betterbiomeblend;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BiomeCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> hash;
    public final Stack<BiomeChunk>                        free;

    public
    BiomeCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        for (int index = 0;
            index < count;
            ++index)
        {
            free.add(new BiomeChunk());
        }
    }

    public void
    releaseChunkWithoutLock(BiomeChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            free.push(chunk);
        }
    }

    public void
    releaseChunk(BiomeChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            lock.lock();

            free.push(chunk);

            lock.unlock();
        }
    }

    public void
    invalidateAll()
    {
        lock.lock();

        for (BiomeChunk chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

            chunk.markAsInvalid();
        }

        hash.clear();

        lock.unlock();
    }

    public void
    invalidateSmallNeighborhood(int chunkX, int chunkZ)
    {
        lock.lock();

        for (int chunkIndex = 0;
            chunkIndex < 9;
            ++chunkIndex)
        {
            int offsetX = ColorBlending.getNeighborOffsetX(chunkIndex);
            int offsetZ = ColorBlending.getNeighborOffsetZ(chunkIndex);

            long key = ColorCaching.getChunkKey(chunkX + offsetX, chunkZ + offsetZ, 0);

            if (chunkIndex == 0)
            {
                BiomeChunk chunk = hash.remove(key);

                if (chunk != null)
                {
                    releaseChunkWithoutLock(chunk);

                    chunk.markAsInvalid();
                }
            }
            else
            {
                BiomeChunk chunk = hash.get(key);

                if (chunk != null)
                {
                    int minX = ColorBlending.getNeighborRectMinX(chunkIndex, 2);
                    int minZ = ColorBlending.getNeighborRectMinZ(chunkIndex, 2);
                    int maxX = ColorBlending.getNeighborRectMaxX(chunkIndex, 2);
                    int maxZ = ColorBlending.getNeighborRectMaxZ(chunkIndex, 2);

                    for (int z1 = minZ;
                        z1 < maxZ;
                        ++z1)
                    {
                        for (int x1 = minX;
                            x1 < maxX;
                            ++x1)
                        {
                            chunk.data[16 * z1 + x1] = null;
                        }
                    }
                }
            }
        }

        lock.unlock();
    }

    public BiomeChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkZ)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkZ, 0);

        lock.lock();

        BiomeChunk result = hash.getAndMoveToFirst(key);

        if (result == null)
        {
            if (!free.empty())
            {
                result = free.pop();
            }
            else
            {
                for (;;)
                {
                    long lastKey = hash.lastLongKey();

                    result = hash.removeLast();

                    if (result.getReferenceCount() == 1)
                    {
                        result.release();
                        break;
                    }
                    else
                    {
                        hash.putAndMoveToFirst(lastKey, result);
                    }
                }
            }

            result.key = key;

            Arrays.fill(result.data, null);

            result.acquire();

            hash.putAndMoveToFirst(result.key, result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
