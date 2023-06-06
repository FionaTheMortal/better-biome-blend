package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.ColorCaching;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public abstract class SliceCache<T extends Slice>
{
    public final static int BUCKET_COUNT = 8;

    public final Long2ObjectLinkedOpenHashMap<T>[] hashList;
    public final StampedLock[]                     lockList;

    public final int sliceCount;
    public       int sliceSize;

    public abstract T newSlice(int sliceSize, int salt);

    public
    SliceCache(int count)
    {
        this.sliceCount = count;

        int countPerHash = count / BUCKET_COUNT;

        hashList = new Long2ObjectLinkedOpenHashMap[BUCKET_COUNT];

        for (int hashIndex = 0;
             hashIndex < BUCKET_COUNT;
             ++hashIndex)
        {
            hashList[hashIndex] = new Long2ObjectLinkedOpenHashMap<>(countPerHash);
        }

        lockList = new StampedLock[BUCKET_COUNT];

        for (int hashIndex = 0;
             hashIndex < BUCKET_COUNT;
             ++hashIndex)
        {
            lockList[hashIndex] = new StampedLock();
        }
    }

    public final void
    reallocSlices(int sliceSize)
    {
        this.sliceSize = sliceSize;

        int countPerHash = this.sliceCount / BUCKET_COUNT;

        for (int hashIndex = 0;
             hashIndex < BUCKET_COUNT;
             ++hashIndex)
        {
            StampedLock                     lock = lockList[hashIndex];
            Long2ObjectLinkedOpenHashMap<T> hash = hashList[hashIndex];

            long stamp = lock.writeLock();

            hash.clear();

            for (int index = 0;
                 index < countPerHash;
                 ++index)
            {
                T slice = newSlice(sliceSize, index);

                hash.put(slice.key, slice);
            }

            lock.unlockWrite(stamp);
        }
    }

    public final void
    invalidateAll(int blendRadius)
    {
        this.sliceSize = BlendConfig.getSliceSize(blendRadius);

        reallocSlices(sliceSize);
    }

    private int
    getBucketIndex(int x, int y, int z)
    {
        int result = (x ^ y ^ z) & (BUCKET_COUNT - 1);

        return result;
    }

    public final void
    releaseRegion(T[] slices)
    {
        for (int index = 0;
             index < 27;
             ++index)
        {
            T slice = slices[index];

            slice.release();
        }
    }

    public final void
    getOrInitRegion(T[] result, int sliceSize, int sliceX, int sliceY, int sliceZ, int colorType)
    {
        int sliceIndex = 0;

        for (int offsetZ = -1;
             offsetZ <= 1;
             ++offsetZ)
        {
            for (int offsetY = -1;
                 offsetY <= 1;
                 ++offsetY)
            {
                for (int offsetX = -1;
                     offsetX <= 1;
                     ++offsetX)
                {
                    final int neighborSliceX = sliceX + offsetX;
                    final int neighborSliceY = sliceY + offsetY;
                    final int neighborSliceZ = sliceZ + offsetZ;

                    long key = ColorCaching.getChunkKey(neighborSliceX, neighborSliceY, neighborSliceZ, colorType);

                    int bucket = getBucketIndex(neighborSliceX, neighborSliceY, neighborSliceZ);

                    StampedLock                     lock = lockList[bucket];
                    Long2ObjectLinkedOpenHashMap<T> hash = hashList[bucket];

                    long stamp = lock.writeLock();

                    T slice = hash.getAndMoveToFirst(key);

                    if (slice == null)
                    {
                        for (;;)
                        {
                            slice = hash.removeLast();

                            if (slice.getRefCount() == 0)
                            {
                                break;
                            }
                            else
                            {
                                hash.putAndMoveToFirst(slice.key, slice);
                            }
                        }

                        slice.key = key;
                        slice.invalidateData();

                        hash.putAndMoveToFirst(slice.key, slice);
                    }

                    if (slice.size == sliceSize)
                    {
                        slice.acquire();
                    }
                    else
                    {
                        slice = newSlice(sliceSize, 0);
                    }

                    lock.unlockWrite(stamp);

                    result[sliceIndex++] = slice;
                }
            }
        }
    }

    public final void
    releaseSlice(T slice)
    {
        slice.release();;
    }

    public final T
    getOrInitSlice(int sliceSize, int sliceX, int sliceY, int sliceZ, int colorType, boolean tryLock)
    {
        long key = ColorCaching.getChunkKey(sliceX, sliceY, sliceZ, colorType);

        int bucket = getBucketIndex(sliceX, sliceY, sliceZ);

        StampedLock                     lock = lockList[bucket];
        Long2ObjectLinkedOpenHashMap<T> hash = hashList[bucket];

        T slice = null;

        long stamp = 0;

        if (tryLock)
        {
            stamp = lock.tryWriteLock();
        }
        else
        {
            stamp = lock.writeLock();
        }

        if (stamp != 0)
        {
            slice = hash.getAndMoveToFirst(key);

            if (slice == null)
            {
                for (;;)
                {
                    slice = hash.removeLast();

                    if (slice.getRefCount() == 0)
                    {
                        break;
                    }
                    else
                    {
                        hash.putAndMoveToFirst(slice.key, slice);
                    }
                }

                slice.key = key;
                slice.invalidateData();

                hash.putAndMoveToFirst(slice.key, slice);
            }

            if (slice.size == sliceSize)
            {
                slice.acquire();
            }
            else
            {
                slice = newSlice(sliceSize, 0);
            }

            lock.unlockWrite(stamp);
        }

        return slice;
    }
}
