package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

   /**
    * @author Tellus
    * @reason Decodes X from the dense global Mercator coordinate profile.
    */
   @Overwrite
   public static int getX(long packed) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         return HighYPackedCoordinateProfile.unpackX(packed);
      }

      int leftShift = 64 - (TELLUS$VANILLA_PACKED_Y_LENGTH + TELLUS$VANILLA_HORIZONTAL_BITS) - TELLUS$VANILLA_HORIZONTAL_BITS;
      int rightShift = 64 - TELLUS$VANILLA_HORIZONTAL_BITS;
      return (int)(packed << leftShift >> rightShift);
   }

   /**
    * @author Tellus
    * @reason Decodes Y from the dense global Mercator coordinate profile.
    */
   @Overwrite
   public static int getY(long packed) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         return HighYPackedCoordinateProfile.unpackY(packed);
      }

      return (int)(packed << 64 - TELLUS$VANILLA_PACKED_Y_LENGTH >> 64 - TELLUS$VANILLA_PACKED_Y_LENGTH);
   }

   /**
    * @author Tellus
    * @reason Decodes Z from the dense global Mercator coordinate profile.
    */
   @Overwrite
   public static int getZ(long packed) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         return HighYPackedCoordinateProfile.unpackZ(packed);
      }

      int zOffset = TELLUS$VANILLA_PACKED_Y_LENGTH;
      return unpackSigned(packed, zOffset, TELLUS$VANILLA_HORIZONTAL_BITS);
   }

   /**
    * @author Tellus
    * @reason Encodes positions with the dense global Mercator coordinate profile.
    */
   @Overwrite
   public static long asLong(int x, int y, int z) {
      if (HighYPackedCoordinateProfile.isEnabled()) {
         return HighYPackedCoordinateProfile.packClamped(x, y, z);
      }

      long horizontalMask = (1L << TELLUS$VANILLA_HORIZONTAL_BITS) - 1L;
      long yMask = (1L << TELLUS$VANILLA_PACKED_Y_LENGTH) - 1L;
      int zOffset = TELLUS$VANILLA_PACKED_Y_LENGTH;
      int xOffset = TELLUS$VANILLA_PACKED_Y_LENGTH + TELLUS$VANILLA_HORIZONTAL_BITS;
      return ((long)x & horizontalMask) << xOffset | ((long)y & yMask) | ((long)z & horizontalMask) << zOffset;
   }

   private static int unpackSigned(long packed, int offset, int bits) {
      int leftShift = 64 - offset - bits;
      int rightShift = 64 - bits;
      return (int)(packed << leftShift >> rightShift);
   }
}
