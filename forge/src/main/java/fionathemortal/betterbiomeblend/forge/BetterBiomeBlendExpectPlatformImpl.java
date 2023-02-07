package fionathemortal.betterbiomeblend.forge;

import fionathemortal.betterbiomeblend.BetterBiomeBlendExpectPlatform;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class BetterBiomeBlendExpectPlatformImpl
{
    public static Path
    getConfigDirectory()
    {
        return FMLPaths.CONFIGDIR.get();
    }
}
