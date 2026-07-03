package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendOptions;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("unused")
@Mixin(value = VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen extends OptionsSubScreen
{
    public
    MixinVideoSettingsScreen(Screen screen, Options options, Component component)
    {
        super(screen, options, component);
    }

    @Inject(
        method = "qualityOptions",
        at = @At("RETURN"),
        cancellable = true)
    private static void
    bbb$modifyQualityOptions(Options options, CallbackInfoReturnable<OptionInstance<?>[]> cir)
    {
        OptionInstance<?>[] original = cir.getReturnValue();

        for (int index = 0;
             index < original.length;
             ++index)
        {
            OptionInstance<?> option = original[index];

            if (option == options.biomeBlendRadius())
            {
                original[index] = ((BetterBiomeBlendOptions) options).betterBiomeBlendRadius();
            }
        }

        cir.setReturnValue(original);
    }
}
