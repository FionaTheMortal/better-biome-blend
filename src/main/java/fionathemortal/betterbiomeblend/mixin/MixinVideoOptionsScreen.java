package fionathemortal.betterbiomeblend.mixin;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoOptionsScreen extends OptionsSubScreen
{
    public
    MixinVideoOptionsScreen(Screen screen, Options options, Component component)
    {
        super(screen, options, component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void
    onInit(CallbackInfo ci)
    {
        BetterBiomeBlendClient.replaceBiomeBlendRadiusOption(this);
    }
}
