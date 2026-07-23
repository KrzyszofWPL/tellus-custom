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
   private static final int TELLUS$VANILLA_HORIZONTAL_BITS = 26;
   // 64 - 26 - 26 = 12; stored as a constant to avoid reading the obfuscated field in fallback paths
   private static final int TELLUS$VANILLA_PACKED_Y_LENGTH = 64 - TELLUS$VANILLA_HORIZONTAL_BITS - TELLUS$VANILLA_HORIZONTAL_BITS;

   @Inject(
      method = "<clinit>",
      at = @At("TAIL")
   )
   private static void tellus$installHighYProfile(CallbackInfo ci) {
      if (!HighYPackedCoordinateProfile.isEnabled()) {
         return;
      }
      // @Shadow on a static final field does not generate a refmap entry reliably under
      // net.neoforged.moddev.legacyforge, so we locate BlockPos.PACKED_Y_LENGTH at runtime
      // by scanning for the unique static final int with the vanilla value (12 = 64-26-26).
      // Mixin-injected fields are named with the TELLUS$ prefix and are excluded from the scan.
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
            if (unsafe.getInt(base, offset) == TELLUS$VANILLA_PACKED_Y_LENGTH) {
               unsafe.putInt(base, offset, HighYPackedCoordinateProfile.Y_BITS);
               return;
            }
         }
         throw new IllegalStateException("Could not locate BlockPos.PACKED_Y_LENGTH field");
      } catch (ReflectiveOperationException e) {
         throw new RuntimeException("Failed to patch BlockPos.PACKED_Y_LENGTH", e);
      }
   }

   @Inject(method = "getX(J)I", at = @At("HEAD"), cancellable = true)
   private static void tellus$getX(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackX(packed));
      }
   }

   @Inject(method = "getY(J)I", at = @At("HEAD"), cancellable = true)
   private static void tellus$getY(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackY(packed));
      }
   }

   @Inject(method = "getZ(J)I", at = @At("HEAD"), cancellable = true)
   private static void tellus$getZ(long packed, CallbackInfoReturnable<Integer> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.unpackZ(packed));
      }
   }

   @Inject(method = "asLong(III)J", at = @At("HEAD"), cancellable = true)
   private static void tellus$asLong(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         cir.setReturnValue(HighYPackedCoordinateProfile.packClamped(x, y, z));
      }
   }
}
