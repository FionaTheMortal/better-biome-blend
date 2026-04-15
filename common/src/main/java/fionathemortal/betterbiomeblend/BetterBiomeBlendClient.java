package fionathemortal.betterbiomeblend;

import fionathemortal.betterbiomeblend.common.accessor.MixinOptionsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ProgressOption;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;

public final class BetterBiomeBlendClient
{
    public static final ProgressOption betterBiomeBlendRadius = new ProgressOption(
        "options.biomeBlendRadius",
        0.0F,
        14.0F,
        1.0F,
        (options) -> (double)((MixinOptionsAccessor)options).bbb$getBetterBiomeBlendRadius(),
        (options, double_) -> {
            ((MixinOptionsAccessor)options).bbb$setBetterBiomeBlendRadius((int)Mth.clamp(double_, 0, 14));
            Minecraft.getInstance().levelRenderer.allChanged();
        },
        (options, progressOption) -> {
            double d = progressOption.get(options);
            int i = (int)d * 2 + 1;
            return new TranslatableComponent(
                "options.generic_value",
                new TranslatableComponent("options.biomeBlendRadius"),
                new TranslatableComponent("options.biomeBlendRadius." + i));
    });

    public static int
    getBlendRadiusSetting()
    {
        return (int)betterBiomeBlendRadius.get(Minecraft.getInstance().options);
    }
}
