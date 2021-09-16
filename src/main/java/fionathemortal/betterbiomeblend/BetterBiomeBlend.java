package fionathemortal.betterbiomeblend;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod(BetterBiomeBlend.MOD_ID)
public final class BetterBiomeBlend
{
    public static final String MOD_ID = "betterbiomeblend";

    public
    BetterBiomeBlend()
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

                    BetterBiomeBlendClient.overwriteOptifineGUIBlendRadiusOption();
                }
            );
    }
}
