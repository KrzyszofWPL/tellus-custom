package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.UndergroundGenerationDepthPolicy;
import com.yucareux.tellus.worldgen.caves.TellusCaveDepthMapper;
import com.yucareux.tellus.worldgen.caves.TellusVanillaWorldGenerationContext;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla underground placed features sample a fixed absolute Y profile. In a
 * Tellus terrain shell that profile may be far from the local underground
 * range. Sample it against a fixed vanilla surface and project the result into
 * the Tellus terrain shell instead.
 *
 * <p>Projection now spans the full solid shell (down to its support bottom /
 * the world's minimum build height) so ores follow the whole depth of tall
 * mountains and deep custom worlds. Diamonds are the one exception: they are
 * pinned to the deep bedrock roots and never surface inside mountains, matching
 * the requested "emerald/iron/coal in the peaks, diamonds only deep" geology.</p>
 */
@Mixin(HeightRangePlacement.class)
public abstract class HeightRangePlacementMixin {
   /** Diamonds may only generate below this many blocks under sea level. */
   private static final int DEEP_DIAMOND_CEILING_BELOW_SEA = 48;

   @Shadow
   @Final
   private HeightProvider height;

   @Inject(
      method = {"getPositions", "method_14452", "m_213904_"},
      at = @At("HEAD"),
      remap = false,
      cancellable = true
   )
   private void tellus$surfaceRelativeUndergroundHeight(
      PlacementContext context,
      RandomSource random,
      BlockPos origin,
      CallbackInfoReturnable<Stream<BlockPos>> callback
   ) {
      if (!(context.generator() instanceof EarthChunkGenerator earthGenerator)
         || !earthGenerator.settings().usesTerrainShell()) {
         return;
      }

      WorldGenerationContext vanillaContext = new TellusVanillaWorldGenerationContext(
         context.generator(),
         LevelHeightAccessor.create(
            TellusCaveDepthMapper.VANILLA_MIN_Y,
            TellusCaveDepthMapper.VANILLA_MAX_Y - TellusCaveDepthMapper.VANILLA_MIN_Y + 1
         )
      );
      int virtualY = this.height.sample(random, vanillaContext);
      int actualSurfaceY = context.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ()) - 1;
      int actualBottomY = findUsableUndergroundBottom(context, earthGenerator, origin, actualSurfaceY);
      int virtualSurfaceY = TellusCaveDepthMapper.VANILLA_SEA_LEVEL;
      int actualY = TellusCaveDepthMapper.actualYForVirtualFeature(
         virtualY, virtualSurfaceY, actualSurfaceY, actualBottomY
      );

      if (actualY != Integer.MIN_VALUE
         && actualY > earthGenerator.getSeaLevel() - DEEP_DIAMOND_CEILING_BELOW_SEA
         && placesDiamondOre(context)) {
         // Keep diamonds in the deep bedrock roots; never expose them high inside
         // mountains. They still generate wherever the shell reaches this deep.
         callback.setReturnValue(Stream.empty());
         return;
      }

      callback.setReturnValue(
         actualY == Integer.MIN_VALUE ? Stream.empty() : Stream.of(origin.atY(actualY))
      );
   }

   private static boolean placesDiamondOre(PlacementContext context) {
      Optional<PlacedFeature> topFeature = context.topFeature();
      if (topFeature.isEmpty()) {
         return false;
      }

      if (topFeature.get().feature().value().config() instanceof OreConfiguration ore) {
         for (OreConfiguration.TargetBlockState target : ore.targetStates) {
            if (target.state.is(Blocks.DIAMOND_ORE) || target.state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
               return true;
            }
         }
      }

      return false;
   }

   private static int findUsableUndergroundBottom(
      PlacementContext context,
      EarthChunkGenerator generator,
      BlockPos origin,
      int surfaceY
   ) {
      int searchBottom = Math.max(
         generator.getMinY() + 1,
         UndergroundGenerationDepthPolicy.deepestCaveOreY(
            surfaceY, generator.settings().undergroundDepth(), generator.getMinY()
         )
      );
      BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), surfaceY - 1, origin.getZ());
      for (int y = surfaceY - 1; y >= searchBottom; y--) {
         cursor.setY(y);
         if (context.getBlockState(cursor).is(Blocks.BEDROCK)) {
            return Math.min(surfaceY - 1, y + 1);
         }
      }
      return searchBottom;
   }
}
