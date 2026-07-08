package fionathemortal.betterbiomeblend.fabric.compat.sodium;

import fionathemortal.betterbiomeblend.BetterBiomeBlend;
import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SodiumConfigEntryPoint implements ConfigEntryPoint
{
    @Override
    public void
    registerConfigLate(ConfigBuilder builder)
    {
        builder.registerOwnModOptions()
            .formatVersion(version -> "1.4.0")
            .addPage(builder.createOptionPage()
                .setName(Component.translatable("bbb.sodium.general"))
                .addOption(bbb$createBlendRadiusOption(builder)));
    }

    private static OptionBuilder
    bbb$createBlendRadiusOption(ConfigBuilder builder)
    {
        return builder.createIntegerOption(ResourceLocation.fromNamespaceAndPath(BetterBiomeBlend.MOD_ID, "biome_blend_radius"))
            .setName(Component.translatable("bbb.sodium.biomeBlend"))
            .setTooltip(Component.translatable("bbb.biomeBlendRadius.tooltip"))
            .setStorageHandler(() -> Minecraft.getInstance().options.save())
            .setBinding(
                value -> BetterBiomeBlendClient.betterBiomeBlendRadius.set(value),
                () -> BetterBiomeBlendClient.betterBiomeBlendRadius.get())
            .setDefaultValue(2)
            .setRange(0, 14, 1)
            .setValueFormatter(value -> {
                int diameter = value * 2 + 1;
                return Component.literal(diameter + "x" + diameter);
            })
            .setImpact(OptionImpact.LOW)
            .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD);
    }
}
