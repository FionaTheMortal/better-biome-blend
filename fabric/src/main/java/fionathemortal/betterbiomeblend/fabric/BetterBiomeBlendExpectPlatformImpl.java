package fionathemortal.betterbiomeblend.fabric;

import fionathemortal.betterbiomeblend.BetterBiomeBlendExpectPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class BetterBiomeBlendExpectPlatformImpl
{
    public static Path
    getConfigDirectory()
    {
        return FabricLoader.getInstance().getConfigDir();
    }
}
