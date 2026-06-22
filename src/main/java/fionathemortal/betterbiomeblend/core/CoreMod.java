package fionathemortal.betterbiomeblend.core;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.launchwrapper.Launch;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import fionathemortal.betterbiomeblend.BetterBiomeBlend;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.Name("Better Biome Blend")
public class CoreMod implements IFMLLoadingPlugin {

    public static boolean foundMixinFramework;

    public CoreMod() {
        foundMixinFramework = true;

        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.betterbiomeblend.json");
    }

    private static boolean isOptifinePresent() {
        try {
            return Launch.classLoader.getClassBytes("net.optifine.CustomColors") != null;
        } catch (IOException ignored) {
            BetterBiomeBlend.LOGGER.info("OptiFine not found. Skipping OptiFine mixins.");
            return false;
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
