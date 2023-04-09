package fionathemortal.betterbiomeblend.forge;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(BetterBiomeBlend.MOD_ID)
public class BetterBiomeBlendForge
{
    public BetterBiomeBlendForge()
    {
        ModLoadingContext.get().registerExtensionPoint(
            IExtensionPoint.DisplayTest.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(
                () -> "client-only",
                (v, n) -> n)
        );

        DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () ->
                () ->
                {
                    MinecraftForge.EVENT_BUS.register(BetterBiomeBlendClient.class);
                }
        );

        BetterBiomeBlend.init();
    }
}
