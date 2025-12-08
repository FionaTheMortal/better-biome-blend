package fionathemortal.betterbiomeblend.common.cache;

import fionathemortal.betterbiomeblend.common.BlendConfig;
import fionathemortal.betterbiomeblend.common.util.Utility;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import java.util.concurrent.locks.StampedLock;

public abstract class SliceCache<T extends Slice>
{
    public final static int BUCKET_COUNT = 8;

    public final Long2ObjectLinkedOpenHashMap<T>[] hashList = new Long2ObjectLinkedOpenHashMap[BUCKET_COUNT];
    public final StampedLock[]                     lockList = new StampedLock[BUCKET_COUNT];

    public final int sliceCount;
    public       int sliceSize;

    public abstract T newSlice(int sliceSize, int salt);

    public
    SliceCache(int sliceCount)
    {
        this.sliceCount = sliceCount;

        int sliceCountPerBucket = sliceCount / BUCKET_COUNT;

        for (int bucket = 0;
             bucket < BUCKET_COUNT;
             ++bucket)
        {
            hashList[bucket] = new Long2ObjectLinkedOpenHashMap<>(sliceCountPerBucket);
        }

        for (int bucket = 0;
             bucket < BUCKET_COUNT;
             ++bucket)
        {
            lockList[bucket] = new StampedLock();
        }
    }

    public final void
    reallocSlices(int sliceSize)
    {
        this.sliceSize = sliceSize;

        int sliceCountPerBucket = this.sliceCount / BUCKET_COUNT;

        for (int bucket = 0;
             bucket < BUCKET_COUNT;
             ++bucket)
        {
            StampedLock                     lock = lockList[bucket];
            Long2ObjectLinkedOpenHashMap<T> hash = hashList[bucket];

            long stamp = lock.writeLock();

            hash.clear();

            for (int index = 0;
                 index < sliceCountPerBucket;
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
        this.sliceSize = BlendConfig.getBlendConfigForBlendRadius(blendRadius).sliceSize;

        reallocSlices(sliceSize);
    }

    private int
    getBucketIndex(int x, int y, int z)
    {
        int result = (x ^ y ^ z) & (BUCKET_COUNT - 1);

        return result;
    }

    public final void
    releaseSlice(T slice)
    {
        slice.release();
    }

    public final T
    getOrInitSlice(int sliceSize, int sliceX, int sliceY, int sliceZ, int colorType, boolean tryLock)
    {
        long key = Utility.getChunkKey(sliceX, sliceY, sliceZ, colorType);

        int bucket = getBucketIndex(sliceX, sliceY, sliceZ);

        StampedLock                     lock = lockList[bucket];
        Long2ObjectLinkedOpenHashMap<T> hash = hashList[bucket];

        T slice = null;

        long stamp;

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
