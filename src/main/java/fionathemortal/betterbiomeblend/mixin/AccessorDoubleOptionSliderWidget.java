package fionathemortal.betterbiomeblend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.widget.DoubleOptionSliderWidget;
import net.minecraft.client.options.DoubleOption;

@Mixin(DoubleOptionSliderWidget.class)
public interface AccessorDoubleOptionSliderWidget
{
    @Accessor()
    DoubleOption getOption();
}
