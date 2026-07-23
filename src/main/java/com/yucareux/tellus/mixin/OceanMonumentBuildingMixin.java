package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(OceanMonumentPieces.MonumentBuilding.class)
public class OceanMonumentBuildingMixin {

   @Redirect(
      method = {"postProcess", "m_213694_"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Math;max(II)I"
      ),
      require = 0
   )
   private int tellus$useTellusSeaLevel(int seaLevel, int minimumSeaLevel) {
      // W miejsce Math.max(seaLevel, minimumSeaLevel) zwracamy po prostu seaLevel
      return seaLevel;
   }
}