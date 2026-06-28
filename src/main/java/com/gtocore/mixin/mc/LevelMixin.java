package com.gtocore.mixin.mc;

import com.gtocore.common.saved.VoidWorldTimeSavedData;

import com.gtolib.api.data.GTODimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Shadow
    public abstract boolean isClientSide();

    @Shadow
    public abstract ResourceKey<Level> dimension();

    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void getVoidWorldDayTime(CallbackInfoReturnable<Long> cir) {
        if (!isClientSide() && GTODimensions.isVoid(dimension()) && VoidWorldTimeSavedData.INSTANCE.isFixedTime()) {
            cir.setReturnValue(1000L);
        }
    }
}
