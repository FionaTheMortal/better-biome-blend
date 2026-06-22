package fionathemortal.betterbiomeblend.core;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Better Biome Blend")
public class CoreMod implements IFMLLoadingPlugin
{
    public static boolean foundMixinFramework;

    public CoreMod()
    {
        foundMixinFramework = true;

        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.betterbiomeblend.json");

        if (isOptifinePresent())
        {
            Mixins.addConfiguration("mixins.betterbiomeblend.optifine.json");
        }
    }

    private static boolean
    isOptifinePresent()
    {
        try
        {
            return Launch.classLoader.getClassBytes("net.optifine.CustomColors") != null;
        }
        catch (IOException ignored)
        {
            BetterBiomeBlend.LOGGER.info("OptiFine not found. Skipping OptiFine mixins.");
            return false;
        }
    }

    @Override
    public String[]
    getASMTransformerClass()
    {
        return new String[0];
    }

    @Override
    public String
    getModContainerClass()
    {
        return null;
    }

    @Nullable
    @Override
    public String
    getSetupClass()
    {
        return null;
    }

    @Override
    public void
    injectData(Map<String, Object> data)
    {
    }

    @Override
    public String
    getAccessTransformerClass()
    {
        return null;
    }
}
