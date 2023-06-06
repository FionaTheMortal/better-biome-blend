package fionathemortal.betterbiomeblend;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fionathemortal.betterbiomeblend.common.debug.Debug;
import fionathemortal.betterbiomeblend.common.debug.DebugSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class BetterBiomeBlendClient
{
    public static OptionInstance<Integer> betterBiomeBlendRadius = new OptionInstance<>(
        "options.biomeBlendRadius",
        OptionInstance.noTooltip(),
        (component, integer) -> {
            int diameter = integer * 2 + 1;
            return Options.genericValueLabel(component, Component.translatable("options.biomeBlendRadius." + diameter));
        },
        new OptionInstance.IntRange(0, 14),
        14,
        (integer) -> {
            Minecraft.getInstance().levelRenderer.allChanged();
        });

    public static int
    getBiomeBlendRadius()
    {
        return betterBiomeBlendRadius.get();
    }

    public static void
    registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
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
                            String.format("Total Subevent CPU time: %.2f ms", summary.totalSubeventCPUTimeInMilliseconds),
                            String.format("Avg. Subevent CPU Time: %.2f ns", summary.averageSubeventTime),
                            String.format("Avg. Subevent 1%%: %.2f ns", summary.averageSubeventOnePercent),
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
}
