package fionathemortal.betterbiomeblend.core;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import net.minecraftforge.fml.relauncher.IFMLCallHook;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import java.util.Map;

/* NOTE:
 * This is heavily based on https://github.com/Fuzss/aquaacrobatics
 * Thanks to Fuzss for making their mod Public Domain
 */
public class CoreModMixinInterface implements IFMLCallHook
{
    @Override
    public void
    injectData(Map<String, Object> map)
    {
    }

    @Override
    public Void
    call() throws Exception
    {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins." + BetterBiomeBlend.MOD_ID + ".json");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");

        return null;
    }
}
