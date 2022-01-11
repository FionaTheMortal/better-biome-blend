package fionathemortal.betterbiomeblend.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class BiomeCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> hash;
    public final Stack<BiomeChunk>                        free;
    public final Long2ObjectLinkedOpenHashMap<BiomeChunk> invalidationHash;

    public
    BiomeCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        invalidationHash = new Long2ObjectLinkedOpenHashMap<>(count / 2);

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

            chunk.prev = null;
            chunk.next = null;

            chunk.markAsInvalid();
        }

        hash.clear();

        invalidationHash.clear();

        lock.unlock();
    }

    private static final byte[]
    invalidationRectParams =
    {
            3, 4,
            0, 4,
            0, 0
    };

    public static int
    getInvalidationRectMin(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidationRectParams[offset + 0];

        return result;
    }

    public static int
    getInvalidationRectMax(int index)
    {
        int offset = 2 * (index + 1);
        int result = invalidationRectParams[offset + 1];

        return result;
    }

    public void
    addToInvalidationHash(BiomeChunk chunk)
    {
        BiomeChunk otherChunk = invalidationHash.get(chunk.invalidationKey);

        if (otherChunk != null)
        {
            chunk.next = otherChunk.next;
            chunk.prev = otherChunk;

            if (otherChunk.next != null)
            {
                otherChunk.next.prev = chunk;
            }

            otherChunk.next = chunk;
        }
        else
        {
            invalidationHash.put(chunk.invalidationKey, chunk);
        }
    }

    public void
    removeFromInvalidationHash(BiomeChunk chunk)
    {
        if (chunk.prev == null)
        {
            invalidationHash.remove(chunk.invalidationKey);

            if (chunk.next != null)
            {
                invalidationHash.put(chunk.invalidationKey, chunk.next);
            }
        }

        chunk.removeFromLinkedList();
    }

    public void
    invalidateSmallNeighborhood(int chunkX, int chunkZ)
    {
        lock.lock();

        for (int z = -1;
             z <= 1;
             ++z)
        {
            for (int x = -1;
                 x <= 1;
                 ++x)
            {
                long key = ColorCaching.getChunkKey(chunkX + x, 0, chunkZ + z, 0);

                BiomeChunk first = invalidationHash.get(key);

                for (BiomeChunk current = first;
                     current != null;
                    )
                {
                    BiomeChunk next = current.next;

                    if (next == first)
                    {
                        int i = 0;
                    }

                    if (x == 0 && z == 0)
                    {
                        hash.remove(current.key);

                        removeFromInvalidationHash(current);

                        releaseChunkWithoutLock(current);

                        current.markAsInvalid();
                    }
                    else
                    {
                        int minX = getInvalidationRectMin(x);
                        int minY = 0;
                        int minZ = getInvalidationRectMin(z);

                        int maxX = getInvalidationRectMax(x);
                        int maxY = 4;
                        int maxZ = getInvalidationRectMax(z);

                        int cacheDim = 4;

                        for (int y1 = minY;
                             y1 < maxY;
                             ++y1)
                        {
                            for (int z1 = minZ;
                                 z1 < maxZ;
                                 ++z1)
                            {
                                for (int x1 = minX;
                                     x1 < maxX;
                                     ++x1)
                                {
                                    int cacheIndex = ColorBlending.getCacheArrayIndex(cacheDim, x1, y1, z1);

                                    current.data[cacheIndex] = null;
                                }
                            }
                        }
                    }

                    current = next;
                }
            }
        }

        lock.unlock();
    }

    public BiomeChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkY, int chunkZ)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, 0);

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

                removeFromInvalidationHash(result);
            }

            long invalidationKey = ColorCaching.getChunkKey(chunkX, 0, chunkZ, 0);

            result.key = key;
            result.invalidationKey = invalidationKey;

            if (result.prev != null || result.next != null)
            {
                int i = 0;
            }

            result.prev = null;
            result.next = null;

            Arrays.fill(result.data, null);

            result.acquire();

            hash.putAndMoveToFirst(result.key, result);

            addToInvalidationHash(result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
