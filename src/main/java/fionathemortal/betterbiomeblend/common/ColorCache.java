package fionathemortal.betterbiomeblend.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.Arrays;
import java.util.Stack;
import java.util.concurrent.locks.ReentrantLock;

public final class ColorCache
{
    public final ReentrantLock                            lock;
    public final Long2ObjectLinkedOpenHashMap<ColorChunk> hash;
    public final Stack<ColorChunk>                        free;
    public final Long2ObjectLinkedOpenHashMap<ColorChunk> invalidationHash;

    public
    ColorCache(int count)
    {
        lock = new ReentrantLock();
        hash = new Long2ObjectLinkedOpenHashMap<>(count);
        free = new Stack<>();

        invalidationHash = new Long2ObjectLinkedOpenHashMap<>(count / 2);

        for (int index = 0;
            index < count;
            ++index)
        {
            free.add(new ColorChunk());
        }
    }

    public void
    releaseChunkWithoutLock(ColorChunk chunk)
    {
        int refCount = chunk.release();

        if (refCount == 0)
        {
            free.push(chunk);
        }
    }

    public void
    releaseChunk(ColorChunk chunk)
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

        for (ColorChunk chunk : hash.values())
        {
            releaseChunkWithoutLock(chunk);

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
    invalidateSmallNeighborhood(int chunkX, int chunkZ)
    {
        lock.lock();

        for (int z = -1;
            z <= 0;
            ++z)
        {
            for (int x = -1;
                x <= 0;
                ++x)
            {
                long key = ColorCaching.getChunkKey(chunkX + x, 0, chunkZ + z, 0);

                ColorChunk first = invalidationHash.get(key);

                if (x == 0 && z == 0)
                {
                    invalidationHash.remove(key);
                }

                ColorChunk next = null;

                for (ColorChunk current = first;
                    current != null;
                    current = next)
                {
                    next = current.next;

                    if (x == 0 && z == 0)
                    {
                        hash.remove(current.key);

                        current.prev = null;
                        current.next = null;

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

                                    current.data[3 * cacheIndex + 0] = (byte)-1;
                                    current.data[3 * cacheIndex + 1] = (byte)-1;
                                    current.data[3 * cacheIndex + 2] = (byte)-1;
                                }
                            }
                        }
                    }
                }
            }
        }

        lock.unlock();
    }

    public void
    addToInvalidationHash(ColorChunk chunk)
    {
        ColorChunk otherChunk = invalidationHash.get(chunk.invalidationKey);

        if (otherChunk != null)
        {
            chunk.next = otherChunk;
            otherChunk.prev = chunk;
        }
        else
        {
            invalidationHash.put(chunk.invalidationKey, chunk);
        }
    }

    public void
    removeFromInvalidationHash(ColorChunk chunk)
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

    public ColorChunk
    getOrDefaultInitializeChunk(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        long key = ColorCaching.getChunkKey(chunkX, chunkY, chunkZ, colorType);

        lock.lock();

        ColorChunk result = hash.getAndMoveToFirst(key);

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

            Arrays.fill(result.data, (byte)-1);

            result.acquire();

            hash.putAndMoveToFirst(result.key, result);

            addToInvalidationHash(result);
        }

        result.acquire();

        lock.unlock();

        return result;
    }
}
