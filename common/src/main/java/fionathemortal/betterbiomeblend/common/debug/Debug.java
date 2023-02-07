package fionathemortal.betterbiomeblend.common.debug;

import java.util.ArrayList;
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
        int count = eventCount;

        long startTime = Long.MAX_VALUE;
        long endTime   = Long.MIN_VALUE;

        for (int index = 0;
            index < count;
            ++index)
        {
            DebugEvent event = events.get(index);

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

        events.sort(
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

        double averageTime       = getAverageElapsedTime(events, count);
        double averageOnePercent = getAverageElapsedTime(events, (count + 99) / 100);

        DebugSummary result = new DebugSummary();

        result.averageTime                = averageTime;
        result.averageOnePercentTime      = averageOnePercent;
        result.callsPerSecond             = (double)(count) / (double)(elapsedTime) * 1e9;
        result.totalCalls                 = count;
        result.elapsedWallTime            = elapsedTime;
        result.elapsedWallTimeInSeconds   = (double)elapsedTime * 1e-9;
        result.totalCPUTimeInMilliseconds = (double)averageTime * (double)count * 1e-6;

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
    pushGenBegin(int chunkX, int chunkY, int chunkZ, int colorType)
    {
        DebugEvent frame = pushDebugEvent();

        frame.startTime = System.nanoTime();

        frame.chunkX = chunkX;
        frame.chunkY = chunkY;
        frame.chunkZ = chunkZ;
        frame.colorType = colorType;

        return frame;
    }

    public static void
    pushGenEnd(DebugEvent frame)
    {
        frame.endTime = System.nanoTime();
    }
}
