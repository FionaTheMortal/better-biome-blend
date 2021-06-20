package fionathemortal.betterbiomeblend;

import net.fabricmc.api.ModInitializer;

public class BetterBiomeBlend implements ModInitializer
{
    public static final String MOD_ID = "betterbiomeblend";

    @Override
    public void
    onInitialize()
    {
        // ScreenEvents.AFTER_INIT.register(BetterBiomeBlendClient::postInitGUIEvent);

        BetterBiomeBlendClient.overwriteOptifineGUIBlendRadiusOption();
    }
}
