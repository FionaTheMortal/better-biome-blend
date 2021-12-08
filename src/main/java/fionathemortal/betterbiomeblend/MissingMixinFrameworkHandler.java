package fionathemortal.betterbiomeblend;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/* NOTE:
 * This is heavily based on https://github.com/Fuzss/aquaacrobatics
 * Thanks to Fuzss for making their mod Public Domain
 */
public class MissingMixinFrameworkHandler
{
    @SubscribeEvent
    public void
    onGuiOpen(final GuiOpenEvent event) {

        if (event.getGui() instanceof GuiMainMenu) {

            event.setGui(new MissingMixinFrameworkGUI(event.getGui()));
            MinecraftForge.EVENT_BUS.unregister(this);
        }
    }
}
