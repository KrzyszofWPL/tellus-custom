package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(DimensionType.class)
public abstract class DimensionTypeHighYMixin {

   @Inject(
      method = "<clinit>",
      at = @At("TAIL"),
      remap = false,
      require = 0
   )
   private static void tellus$installShiftedHighYRange(CallbackInfo ci) {
      if (!HighYPackedCoordinateProfile.isEnabled()) return;
      // @Shadow on static final fields does not generate refmap entries under
      // net.neoforged.moddev.legacyforge; locate fields via Unsafe using known vanilla values.
      try {
         Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
         unsafeField.setAccessible(true);
         sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

         // Vanilla 1.20.1 values: Y_SIZE=4096, MIN_Y=-2048, MAX_Y=2047,
         // WAY_BELOW_MIN_Y=-32768 (MIN_Y<<4), WAY_ABOVE_MAX_Y=32752 (MAX_Y<<4)
         patchField(unsafe, 4096,   HighYPackedCoordinateProfile.DIMENSION_Y_SIZE);         // Y_SIZE
         patchField(unsafe, -2048,  HighYPackedCoordinateProfile.DIMENSION_MIN_Y);          // MIN_Y
         patchField(unsafe, 2047,   HighYPackedCoordinateProfile.DIMENSION_MAX_Y);          // MAX_Y
         patchField(unsafe, -32768, HighYPackedCoordinateProfile.DIMENSION_MIN_Y << 4);     // WAY_BELOW_MIN_Y
         patchField(unsafe, 32752,  HighYPackedCoordinateProfile.DIMENSION_MAX_Y << 4);     // WAY_ABOVE_MAX_Y
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException("Failed to patch DimensionType high-Y fields", e);
      }
   }

   private static void patchField(sun.misc.Unsafe unsafe, int vanillaValue, int newValue) {
      for (Field field : DimensionType.class.getDeclaredFields()) {
         if (field.getType() != int.class) continue;
         int mods = field.getModifiers();
         if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods)) continue;
         long offset = unsafe.staticFieldOffset(field);
         Object base = unsafe.staticFieldBase(field);
         if (unsafe.getInt(base, offset) == vanillaValue) {
            unsafe.putInt(base, offset, newValue);
            return;
         }
      }
      throw new IllegalStateException("Could not locate DimensionType field with vanilla value " + vanillaValue);
   }
}
