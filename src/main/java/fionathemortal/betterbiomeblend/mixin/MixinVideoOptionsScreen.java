package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fionathemortal.betterbiomeblend.BetterBiomeBlendClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.options.GameOptionsScreen;
import net.minecraft.client.options.GameOptions;
import net.minecraft.text.Text;

@Mixin(VideoOptionsScreen.class)
public abstract class MixinVideoOptionsScreen extends GameOptionsScreen
{
    public MixinVideoOptionsScreen(Screen parent, GameOptions gameOptions, Text title)
    {
        super(parent, gameOptions, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void
    onInit(CallbackInfo ci)
    {
        BetterBiomeBlendClient.replaceBiomeBlendRadiusOption(this);
    }
}
