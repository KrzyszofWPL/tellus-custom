package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@Mixin(BlockPos.class)
public abstract class BlockPosHighYMixin {

   @Inject(
      method = "<clinit>",
      at = @At("TAIL")
   )
   private static void tellus$installHighYProfile(CallbackInfo ci) {
      if (!HighYPackedCoordinateProfile.isEnabled()) return;
      // @Shadow and @Overwrite do not generate refmap entries under
      // net.neoforged.moddev.legacyforge; locate PACKED_Y_LENGTH via Unsafe instead.
      // It is the only static final int field with value 12 (= 64 - 26 - 26) in BlockPos.
      try {
         Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
         unsafeField.setAccessible(true);
         sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

         for (Field field : BlockPos.class.getDeclaredFields()) {
            if (field.getName().startsWith("TELLUS$")) continue;
            if (field.getType() != int.class) continue;
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods)) continue;
            long offset = unsafe.staticFieldOffset(field);
            Object base = unsafe.staticFieldBase(field);
            if (unsafe.getInt(base, offset) == 12) {
               unsafe.putInt(base, offset, HighYPackedCoordinateProfile.Y_BITS);
               return;
            }
         }
         throw new IllegalStateException("Could not locate BlockPos.PACKED_Y_LENGTH field");
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException("Failed to patch BlockPos.PACKED_Y_LENGTH", e);
      }
   }

   @Inject(method = "getX", at = @At("HEAD"), cancellable = true)
   private static void tellus$getX(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackX(packed));
      }
   }

   @Inject(method = "getY", at = @At("HEAD"), cancellable = true)
   private static void tellus$getY(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackY(packed));
      }
   }

   @Inject(method = "getZ", at = @At("HEAD"), cancellable = true)
   private static void tellus$getZ(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackZ(packed));
      }
   }

   @Inject(method = "asLong", at = @At("HEAD"), cancellable = true)
   private static void tellus$asLong(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.packClamped(x, y, z));
      }
   }
}
