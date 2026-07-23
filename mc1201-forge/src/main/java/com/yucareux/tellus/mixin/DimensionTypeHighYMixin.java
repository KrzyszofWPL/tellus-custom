package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DimensionType.class)
public abstract class DimensionTypeHighYMixin {
   @Mutable
   @Shadow
   @Final
   public static int Y_SIZE;

   @Mutable
   @Shadow
   @Final
   public static int MAX_Y;

   @Mutable
   @Shadow
   @Final
   public static int MIN_Y;

   @Mutable
   @Shadow
   @Final
   public static int WAY_ABOVE_MAX_Y;

   @Mutable
   @Shadow
   @Final
   public static int WAY_BELOW_MIN_Y;

   @Inject(
      method = "<clinit>",
      at = @At("TAIL"),
      remap = false,
      require = 0
   )
   private static void tellus$installShiftedHighYRange(CallbackInfo ci) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         Y_SIZE = HighYPackedCoordinateProfile.DIMENSION_Y_SIZE;
         MIN_Y = HighYPackedCoordinateProfile.DIMENSION_MIN_Y;
         MAX_Y = HighYPackedCoordinateProfile.DIMENSION_MAX_Y;
         WAY_BELOW_MIN_Y = MIN_Y << 4;
         WAY_ABOVE_MAX_Y = MAX_Y << 4;
      }
   }
}