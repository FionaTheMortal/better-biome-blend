package fionathemortal.betterbiomeblend.forge.compat.sodium;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge("betterbiomeblend")
public final class SodiumConfigEntryPoint implements ConfigEntryPoint
{
    private static ControlValueFormatter
    bbb$biomeBlendFormatter()
    {
        return (v) -> {
            if (v >= 0 && v <= 14) {
                if (v == 0) {
                    return Component.translatable("gui.none");
                } else {
                    int sv = 2 * v + 1;
                    return Component.translatable("sodium.options.biome_blend.value", new Object[]{sv, sv});
                }
            } else {
                return Component.translatable("parsing.int.invalid", new Object[]{v});
            }
        };
    }

    @Override
    public void registerConfigLate(ConfigBuilder builder)
    {
        Identifier optionId = Identifier.parse("sodium:quality.biome_blend");
        builder.registerOwnModOptions().registerOptionReplacement(optionId,
            builder.createIntegerOption(optionId)
                .setStorageHandler(() -> Minecraft.getInstance().options.save())
                .setName(Component.translatable("bbb.biomeBlendRadius"))
                .setValueFormatter(bbb$biomeBlendFormatter())
                .setTooltip(Component.translatable("bbb.biomeBlendRadius.tooltip"))
                .setRange(0, 14, 1)
                .setDefaultValue(14)
                .setBinding(BetterBiomeBlendClient::setBlendRadiusSetting, BetterBiomeBlendClient::getBlendRadiusSetting)
                .setImpact(OptionImpact.LOW)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD));
    }
}
