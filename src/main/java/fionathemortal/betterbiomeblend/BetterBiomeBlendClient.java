package fionathemortal.betterbiomeblend;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugSummary;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent.InitScreenEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BetterBiomeBlendClient
{
    public static final Logger LOGGER = LogManager.getLogger(BetterBiomeBlend.MOD_ID);

    private static final Component biomeBlendOptionTooltip = Component.translatable("options.biomeBlendRadiusTooltip");

    @SubscribeEvent
    public static void
    preInitGUIEvent(InitScreenEvent.Post event)
    {
        Screen screen = event.getScreen();

        if (screen instanceof VideoSettingsScreen)
        {
            VideoSettingsScreen videoSettingsScreen = (VideoSettingsScreen)screen;

            replaceBiomeBlendRadiusOption(videoSettingsScreen.getMinecraft());
        }
    }

    @SubscribeEvent
    public static void
    registerCommandsEvent(RegisterCommandsEvent event)
    {
       CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

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
                                player.displayClientMessage(
                                    Component.literal("Started benchmark. Stop with /betterbiomeblend toggleBenchmark"),
                                    false);
                            }
                        }
                        else
                        {
                            if (player != null)
                            {
                                player.displayClientMessage(Component.literal("Stopped benchmark"), false);
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
                                    player.displayClientMessage(Component.literal(line), false);
                                }
                            }

                            Debug.teardown();
                        }

                        return 0;
                    }));

        dispatcher.register(benchmarkCommand);
    }

    public static void
    replaceBiomeBlendRadiusOption(Minecraft client)
    {
        client.options.biomeBlendRadius = new OptionInstance(
                "options.biomeBlendRadius", OptionInstance.cachedConstantTooltip(biomeBlendOptionTooltip), (component, integer) -> {
            int i = (int)integer * 2 + 1;
            return Options.genericValueLabel(component, Component.translatable("options.biomeBlendRadius." + i));
        }, new OptionInstance.IntRange(0, 7), 2, (integer) -> {
            Minecraft.getInstance().levelRenderer.allChanged();
        });
    }
}
