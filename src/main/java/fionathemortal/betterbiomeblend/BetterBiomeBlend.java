package fionathemortal.betterbiomeblend;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.VideoSettingsScreen;

public class BetterBiomeBlend implements ModInitializer
{
    public static final String MOD_ID = "betterbiomeblend";

    @Override
    public void
    onInitialize()
    {
        BetterBiomeBlendClient.overwriteOptifineGUIBlendRadiusOption();

        BetterBiomeBlendClient.registerCommands();

        ScreenEvents.AFTER_INIT.register(
            (client, screen, scaledWidth, scaledHeight) ->
            {
                if (screen instanceof VideoSettingsScreen)
                {
                    VideoSettingsScreen videoSettingsScreen = (VideoSettingsScreen)screen;

                    BetterBiomeBlendClient.replaceBiomeBlendRadiusOption(videoSettingsScreen, client);
                }
            });
    }
}
