package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(DimensionType.class)
public abstract class DimensionTypeHighYMixin {

   @Inject(
      method = "<clinit>",
      at = @At("TAIL"),
      remap = false,
      require = 0
   )
   private static void tellus$installShiftedHighYRange(CallbackInfo ci) {
      if (!HighYPackedCoordinateProfile.isEnabled()) {
         return;
      }
      // @Shadow @Mutable @Final on static fields does not reliably generate refmap entries under
      // net.neoforged.moddev.legacyforge. Patch the fields directly via Unsafe instead.
      try {
         Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
         unsafeField.setAccessible(true);
         sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

         patchIntField(unsafe, DimensionType.class, "Y_SIZE", HighYPackedCoordinateProfile.DIMENSION_Y_SIZE);
         patchIntField(unsafe, DimensionType.class, "MIN_Y", HighYPackedCoordinateProfile.DIMENSION_MIN_Y);
         patchIntField(unsafe, DimensionType.class, "MAX_Y", HighYPackedCoordinateProfile.DIMENSION_MAX_Y);
         patchIntField(unsafe, DimensionType.class, "WAY_BELOW_MIN_Y", HighYPackedCoordinateProfile.DIMENSION_MIN_Y << 4);
         patchIntField(unsafe, DimensionType.class, "WAY_ABOVE_MAX_Y", HighYPackedCoordinateProfile.DIMENSION_MAX_Y << 4);
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException("Failed to patch DimensionType high-Y fields", e);
      }
   }

   private static void patchIntField(sun.misc.Unsafe unsafe, Class<?> clazz, String fieldName, int value)
         throws NoSuchFieldException {
      Field field = clazz.getDeclaredField(fieldName);
      long offset = unsafe.staticFieldOffset(field);
      Object base = unsafe.staticFieldBase(field);
      unsafe.putInt(base, offset, value);
   }
}
