package fionathemortal.betterbiomeblend;

import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

public final class BetterBiomeBlendClient
{
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    public static final int BIOME_BLEND_RADIUS_MAX = 14;
    public static final int BIOME_BLEND_RADIUS_MIN = 0;

    public static final ProgressOption BIOME_BLEND_RADIUS = new ProgressOption(
        "options.biomeBlendRadius",
        BIOME_BLEND_RADIUS_MIN,
        BIOME_BLEND_RADIUS_MAX,
        1.0F,
        BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue,
        BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
        BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText);

    public static final Options gameSettings = Minecraft.getInstance().options;

    @SubscribeEvent
    public static void
    postInitGUIEvent(InitGuiEvent.Post event)
    {
        Screen screen = event.getGui();

        if (screen instanceof VideoSettingsScreen)
        {
            VideoSettingsScreen videoSettingsScreen = (VideoSettingsScreen)screen;

            replaceBiomeBlendRadiusOption(videoSettingsScreen);
        }
    }

    public static int
    getBlendRadiusSetting()
    {
        int result = gameSettings.biomeBlendRadius;

        return result;
    }

    @SuppressWarnings("resource")
    public static void
    replaceBiomeBlendRadiusOption(VideoSettingsScreen screen)
    {
        List<? extends GuiEventListener> children = screen.children();

        for (GuiEventListener child : children)
        {
            if (child instanceof OptionsList)
            {
                OptionsList rowList = (OptionsList)child;

                List<net.minecraft.client.gui.components.OptionsList.Entry> rowListEntries = rowList.children();

                boolean replacedOption = false;

                for (int index = 0;
                    index < rowListEntries.size();
                    ++index)
                {
                    net.minecraft.client.gui.components.OptionsList.Entry row = rowListEntries.get(index);

                    List<? extends GuiEventListener> rowChildren = row.children();

                    for (GuiEventListener rowChild : rowChildren)
                    {
                        if (rowChild instanceof AccessorOptionSlider)
                        {
                            AccessorOptionSlider accessor = (AccessorOptionSlider)rowChild;

                            if (accessor.getOption() == Option.BIOME_BLEND_RADIUS)
                            {
                                net.minecraft.client.gui.components.OptionsList.Entry newRow = net.minecraft.client.gui.components.OptionsList.Entry.big(
                                    screen.getMinecraft().options,
                                    screen.width,
                                    BIOME_BLEND_RADIUS);

                                rowListEntries.set(index, newRow);

                                replacedOption = true;
                            }
                        }
                    }

                    if (replacedOption)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static Double
    biomeBlendRadiusOptionGetValue(Options settings)
    {
        double result = (double)settings.biomeBlendRadius;

        return result;
    }

    @SuppressWarnings("resource")
    public static void
    biomeBlendRadiusOptionSetValue(Options settings, Double optionValues)
    {
        /* NOTE: Concurrent modification exception with structure generation
         * But this code is a 1 to 1 copy of vanilla code so it might just be an unlikely bug on their end */

        int currentValue = (int)optionValues.doubleValue();
        int newSetting   = Mth.clamp(currentValue, BIOME_BLEND_RADIUS_MIN, BIOME_BLEND_RADIUS_MAX);

        if (settings.biomeBlendRadius != newSetting)
        {
            settings.biomeBlendRadius = newSetting;

            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    public static Component
    biomeBlendRadiusOptionGetDisplayText(Options settings, ProgressOption optionValues)
    {
        int currentValue  = (int)optionValues.get(settings);
        int blendDiameter = 2 * currentValue + 1;

        Component result = new TranslatableComponent(
            "options.generic_value",
            new TranslatableComponent("options.biomeBlendRadius"),
            new TranslatableComponent("options.biomeBlendRadius." + blendDiameter));

        return result;
    }

    public static void
    overwriteOptifineGUIBlendRadiusOption()
    {
        boolean success = false;

        try
        {
            Class<?> guiDetailSettingsOFClass = Class.forName("net.optifine.gui.GuiDetailSettingsOF");

            try
            {
                Field enumOptionsField = guiDetailSettingsOFClass.getDeclaredField("enumOptions");

                enumOptionsField.setAccessible(true);

                Option[] enumOptions = (Option[])enumOptionsField.get(null);

                boolean found = false;

                for (int index = 0;
                    index < enumOptions.length;
                    ++index)
                {
                    Option option = enumOptions[index];

                    if (option == Option.BIOME_BLEND_RADIUS)
                    {
                        enumOptions[index] = BIOME_BLEND_RADIUS;

                        found = true;

                        break;
                    }
                }

                if (found)
                {
                    success = true;
                }
                else
                {
                    BetterBiomeBlendClient.LOGGER.warn("Optifine GUI option was not found.");
                }
            }
            catch (Exception e)
            {
                BetterBiomeBlendClient.LOGGER.warn(e);
            }
        }
        catch (ClassNotFoundException e)
        {
            BetterBiomeBlendClient.LOGGER.info("Otifine does not seem to be loaded, so no need to overwrite anything.");
        }

        if (success)
        {
            BetterBiomeBlendClient.LOGGER.info("Optifine GUI option was successfully replaced.");
        }
    }
}
