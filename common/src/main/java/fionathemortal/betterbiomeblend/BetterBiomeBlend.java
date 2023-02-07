package fionathemortal.betterbiomeblend;

import fionathemortal.betterbiomeblend.common.BlendCache;
import fionathemortal.betterbiomeblend.common.BlendChunk;

import java.util.concurrent.atomic.AtomicLong;

public class BetterBiomeBlend
{
    public static final String MOD_ID = "betterbiomeblend";

    public static void
    init()
    {
        System.out.println(BetterBiomeBlendExpectPlatform.getConfigDirectory().toAbsolutePath().normalize().toString());
    }
}
