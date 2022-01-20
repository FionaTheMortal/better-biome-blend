package fionathemortal.betterbiomeblend;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugSummary;
import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.ProgressOption;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

public final class BetterBiomeBlendClient
{
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    private static final Component biomeBlendOptionTooltip = new TranslatableComponent("options.biomeBlendRadiusTooltip");

    private static final ProgressOption BIOME_BLEND_RADIUS = new ProgressOption(
        "options.biomeBlendRadius",
        0,
        7,
        1.0F,
        BetterBiomeBlendClient::biomeBlendRadiusOptionGetValue,
        BetterBiomeBlendClient::biomeBlendRadiusOptionSetValue,
        BetterBiomeBlendClient::biomeBlendRadiusOptionGetDisplayText,
        BetterBiomeBlendClient::biomeBlendRadiusOptionGetFormattedTooltip);

    public static List<FormattedCharSequence>
    biomeBlendRadiusOptionGetFormattedTooltip(Minecraft instance)
    {
        List<FormattedCharSequence> result = instance.font.split(biomeBlendOptionTooltip, 200);

        return result;
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
        int newSetting   = Mth.clamp(currentValue, 0, 7);

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
    registerCommands()
    {
        LiteralArgumentBuilder<CommandSourceStack> benchmarkCommand = Commands
            .literal("betterbiomeblend")
            .then(Commands.literal("toggleBenchmark")
            .executes(
                context ->
                {
                    boolean benchmarking = Debug.toggleBenchmark();

                    Player player = Minecraft.getInstance().player;

                    if (benchmarking)
                    {
                        if (player != null)
                        {
                            player.sendMessage(
                                    new TextComponent("Started benchmark. Stop with /betterbiomeblend toggleBenchmark"),
                                    Util.NIL_UUID);
                        }
                    }
                    else
                    {
                        if (player != null)
                        {
                            player.sendMessage(new TextComponent("Stopped benchmark"), Util.NIL_UUID);
                        }

                        DebugSummary summary = Debug.collateDebugEvents();

                        String[] lines =
                        {
                            "",
                            String.format("Call Count: %d"  , summary.totalCalls),
                            String.format("Wall Time: %.2f s"  , summary.elapsedWallTimeInSeconds),
                            String.format("Calls/sec: %.2f", summary.callsPerSecond),
                            String.format("Avg. CPU Time: %.2f ns", summary.averageTime),
                            String.format("Avg. 1%%: %.2f ns", summary.averageOnePercentTime),
                            String.format("Total CPU time: %.2f ms", summary.totalCPUTimeInMilliseconds),
                            ""
                        };

                        if (player != null)
                        {
                            for (String line : lines)
                            {
                                player.sendMessage(new TextComponent(line), Util.NIL_UUID);
                            }
                        }

                        Debug.teardown();
                    }

                    return 0;
                }));

        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> dispatcher.register(benchmarkCommand)));
    }

    public static void
    replaceBiomeBlendRadiusOption(VideoSettingsScreen screen, Minecraft client)
    {
        List<? extends GuiEventListener> children = screen.children();

        for (GuiEventListener child : children)
        {
            if (child instanceof OptionsList)
            {
                OptionsList rowList = (OptionsList) child;

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
                            AccessorOptionSlider accessor = (AccessorOptionSlider) rowChild;

                            if (accessor.getOption() == Option.BIOME_BLEND_RADIUS)
                            {
                                OptionsList.Entry newRow = OptionsList.Entry.big(
                                    client.options,
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
                        // enumOptions[index] = BIOME_BLEND_RADIUS;

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
