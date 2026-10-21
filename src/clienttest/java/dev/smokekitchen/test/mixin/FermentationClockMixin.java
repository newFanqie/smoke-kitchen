package dev.smokekitchen.test.mixin;

import dev.smokekitchen.test.FermentationFlowTest;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Test-only clock matching: Fabric advances its server once per client tick. No production progress is changed. */
@Mixin(Minecraft.class)
abstract class FermentationClockMixin {
    @ModifyReturnValue(method="getTickTargetMillis",at=@At("RETURN"))
    private float fermentationClock(float original){return FermentationFlowTest.fastClock?5:original;}
}
