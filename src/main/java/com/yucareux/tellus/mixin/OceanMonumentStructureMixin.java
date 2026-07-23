package com.yucareux.tellus.mixin;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OceanMonumentStructure.class)
public class OceanMonumentStructureMixin {

   @Inject(
      method = {"generatePieces", "m_228965_"},
      at = @At("RETURN"),
      require = 0
   )
   private static void tellus$preserveShiftedMonumentY(
      Structure.GenerationContext context,
      StructurePiecesBuilder builder,
      CallbackInfo ci
   ) {
   }
}