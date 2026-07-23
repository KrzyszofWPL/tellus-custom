package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelHighYPackedCoordinateMixin {

   @Inject(method = "isInWorldBoundsHorizontal(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
   private static void tellus$isInWorldBoundsHorizontal(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.containsHorizontal(pos.getX(), pos.getZ()));
      }
   }

   @Inject(method = "isOutsideSpawnableHeight(I)Z", at = @At("HEAD"), cancellable = true)
   private static void tellus$isOutsideSpawnableHeight(int y, CallbackInfoReturnable<Boolean> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(y < HighYPackedCoordinateProfile.Y_MIN || y > HighYPackedCoordinateProfile.Y_MAX);
      }
   }
}
