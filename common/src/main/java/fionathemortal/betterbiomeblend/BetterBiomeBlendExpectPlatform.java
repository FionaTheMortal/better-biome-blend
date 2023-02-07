package fionathemortal.betterbiomeblend;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class BetterBiomeBlendExpectPlatform
{
    @ExpectPlatform
    public static Path
    getConfigDirectory()
    {
        throw new AssertionError();
    }
}
