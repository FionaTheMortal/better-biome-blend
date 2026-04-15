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
    getBlendRadiusSetting()
    {
        return betterBiomeBlendRadius.get();
    }
}
