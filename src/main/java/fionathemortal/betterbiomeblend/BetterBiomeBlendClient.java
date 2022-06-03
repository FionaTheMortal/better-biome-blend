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

    public static List<FormattedCharSequence>
    biomeBlendRadiusOptionGetFormattedTooltip(Minecraft instance)
    {
        List<FormattedCharSequence> result = instance.font.split(biomeBlendOptionTooltip, 200);

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
            if (child instanceof OptionsList rowList)
            {
                List<net.minecraft.client.gui.components.OptionsList.Entry> rowListEntries = rowList.children();

                boolean replacedOption = false;

                for (OptionsList.Entry row : rowListEntries)
                {
                    List<? extends GuiEventListener> rowChildren = row.children();

                    for (GuiEventListener rowChild : rowChildren)
                    {
                        if (rowChild instanceof AccessorOptionSlider accessor)
                        {
                            ProgressOption option = accessor.getOption();

                            if (option == Option.BIOME_BLEND_RADIUS)
                            {
                                option.tooltipSupplier = BetterBiomeBlendClient::biomeBlendRadiusOptionGetFormattedTooltip;

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
