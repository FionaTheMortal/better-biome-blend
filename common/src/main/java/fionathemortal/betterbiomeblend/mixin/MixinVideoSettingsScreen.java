package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VideoSettingsScreen.class)
abstract public class MixinVideoSettingsScreen extends OptionsSubScreen
{
    @Shadow
    private OptionsList list;

    public
    MixinVideoSettingsScreen(Screen screen, Options options, Component component)
    {
        super(screen, options, component);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)I"), index = 0)
    private OptionInstance<?>
    modifyAddBig(OptionInstance<?> argument)
    {
        OptionInstance<?> result = argument;

        if (argument == this.options.biomeBlendRadius())
        {
            result = BetterBiomeBlendClient.betterBiomeBlendRadius;
        }

        return result;
    }
}
