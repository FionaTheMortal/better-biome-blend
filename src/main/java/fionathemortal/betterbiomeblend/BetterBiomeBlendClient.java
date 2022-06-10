package fionathemortal.betterbiomeblend;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugSummary;
import fionathemortal.betterbiomeblend.mixin.AccessorOptionSlider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

    private static final Component biomeBlendOptionTooltip = Component.translatable("options.biomeBlendRadiusTooltip");

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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(benchmarkCommand));
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
