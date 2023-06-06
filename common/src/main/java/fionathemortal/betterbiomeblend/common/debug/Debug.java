package fionathemortal.betterbiomeblend.common.debug;

import fionathemortal.betterbiomeblend.common.BlendChunk;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class Debug
{
    public static final int INITIAL_FRAME_COUNT = 12 * 1024;

    public static volatile boolean measurePerformance = false;

    public static int                   eventCount = 0;
    public static ArrayList<DebugEvent> events;
    public static ReentrantLock         lock = new ReentrantLock();

    public static boolean
    toggleBenchmark()
    {
        if (!measurePerformance)
        {
            initialize();

            measurePerformance = true;
        }
        else
        {
            measurePerformance = false;
        }

        return measurePerformance;
    }

    private static double
    getAverageElapsedTime(ArrayList<DebugEvent> events, int count)
    {
        long accumulatedElapsedTime = 0;

        for (int index = 0;
            index < count;
            ++index)
        {
            DebugEvent event = events.get(index);

            long elapsedTime = event.endTime - event.startTime;

            accumulatedElapsedTime += elapsedTime;
        }

        double result = (double)(accumulatedElapsedTime) / (double)(count);

        return result;
    }

    public static DebugSummary
    collateDebugEvents()
    {
        ArrayList<DebugEvent> colorGenEvents = new ArrayList<>();
        ArrayList<DebugEvent> subevents      = new ArrayList<>();

        for (int index = 0;
             index < eventCount;
             ++index)
        {
            DebugEvent event = events.get(index);

            switch (event.eventType)
            {
                case COLOR_GEN:
                {
                    colorGenEvents.add(event);
                } break;
                case SUBEVENT:
                {
                    subevents.add(event);
                } break;
            }
        }

        long startTime = Long.MAX_VALUE;
        long endTime   = Long.MIN_VALUE;

        for (DebugEvent event : colorGenEvents)
        {
            if (event.startTime < startTime)
            {
                startTime = event.startTime;
            }

            if (event.endTime > endTime)
            {
                endTime = event.endTime;
            }
        }

        long elapsedTime = endTime - startTime;

        colorGenEvents.sort(
            (a, b) ->
            {
                long time1 = a.endTime - a.startTime;
                long time2 = b.endTime - b.startTime;

                int result = 0;

                if (time1 != time2)
                {
                    result = (time1 > time2) ? -1 : 1;
                }

                return result;
            });

        subevents.sort(
            (a, b) ->
            {
                long time1 = a.endTime - a.startTime;
                long time2 = b.endTime - b.startTime;

                int result = 0;

                if (time1 != time2)
                {
                    result = (time1 > time2) ? -1 : 1;
                }

                return result;
            });

        int colorGenEventCount = colorGenEvents.size();

        double averageTime       = getAverageElapsedTime(colorGenEvents,  colorGenEventCount);
        double averageOnePercent = getAverageElapsedTime(colorGenEvents, (colorGenEventCount + 99) / 100);

        DebugSummary result = new DebugSummary();

        result.averageTime                = averageTime;
        result.averageOnePercentTime      = averageOnePercent;
        result.callsPerSecond             = (double)(colorGenEventCount) / (double)(elapsedTime) * 1e9;
        result.totalCalls                 = colorGenEventCount;
        result.elapsedWallTime            = elapsedTime;
        result.elapsedWallTimeInSeconds   = (double)elapsedTime * 1e-9;
        result.totalCPUTimeInMilliseconds = (double)averageTime * (double)colorGenEventCount * 1e-6;

        double averageSubeventTime       = getAverageElapsedTime(subevents,  subevents.size());
        double averageSubeventOnePercent = getAverageElapsedTime(subevents, (subevents.size() + 99) / 100);

        result.totalSubeventCPUTimeInMilliseconds = averageSubeventTime * (double)subevents.size() * 1e-6;
        result.averageSubeventTime                = averageSubeventTime;
        result.averageSubeventOnePercent          = averageSubeventOnePercent;

        return result;
    }

    private static void
    initialize()
    {
        lock.lock();

        events = new ArrayList<>(INITIAL_FRAME_COUNT);

        for (int index = 0;
            index < INITIAL_FRAME_COUNT;
            ++index)
        {
            DebugEvent frame = new DebugEvent();

            events.add(frame);
        }

        lock.unlock();
    }

    public static void
    teardown()
    {
        events = null;
        eventCount = 0;
    }

    private static void
    growEventBuffer()
    {
        int oldSize = events.size();
        int newSize = 2 * oldSize;

        events.ensureCapacity(newSize);

        for (int index = oldSize;
             index < newSize;
             ++index)
        {
            DebugEvent frame = new DebugEvent();

            events.add(frame);
        }
    }

    private static DebugEvent
    pushDebugEvent()
    {
        lock.lock();

        if (eventCount >= events.size())
        {
            growEventBuffer();
        }

        DebugEvent result = events.get(eventCount);

        ++eventCount;

        lock.unlock();

        return result;
    }

    public static DebugEvent
    pushColorGenEvent(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        DebugEvent event = null;

        if (Debug.measurePerformance)
        {
            event = pushDebugEvent();

            event.eventType = DebugEventType.COLOR_GEN;

            event.startTime = System.nanoTime();

            event.chunkX = chunkX;
            event.chunkY = chunkY;
            event.chunkZ = chunkZ;
            event.colorType = colorType;
        }

        return event;
    }

    public static DebugEvent
    pushSubevent(DebugEventType eventType)
    {
        DebugEvent event = null;

        if (Debug.measurePerformance)
        {
            event = pushDebugEvent();

            event.eventType = eventType;
            event.startTime = System.nanoTime();
        }

        return event;
    }

    public static void
    endEvent(DebugEvent event)
    {
        if (event != null)
        {
            event.endTime = System.nanoTime();
        }
    }

    public static AtomicLong colorTypeHit  = new AtomicLong();
    public static AtomicLong colorTypeMiss = new AtomicLong();

    public static void
    countColorType(int colorType, int lastColorType)
    {
        if (colorType == lastColorType)
        {
            colorTypeHit.getAndIncrement();
        }
        else
        {
            colorTypeMiss.getAndIncrement();
        }
    }

    public static AtomicLong threadLocalHit  = new AtomicLong();
    public static AtomicLong threadLocalMiss = new AtomicLong();

    public static void
    countThreadLocalChunk(BlendChunk chunk)
    {
        if (chunk != null)
        {
            threadLocalHit.getAndIncrement();
        }
        else
        {
            threadLocalMiss.getAndIncrement();
        }
    }

    public static AtomicLong blendCacheHit  = new AtomicLong();
    public static AtomicLong blendCacheMiss = new AtomicLong();

    public static void
    countBlendCache(BlendChunk chunk)
    {
        if (chunk != null)
        {
            blendCacheHit.getAndIncrement();
        }
        else
        {
            blendCacheMiss.getAndIncrement();
        }
    }
}
