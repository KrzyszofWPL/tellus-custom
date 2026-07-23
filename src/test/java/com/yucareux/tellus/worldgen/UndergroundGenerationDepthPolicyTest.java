package com.yucareux.tellus.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UndergroundGenerationDepthPolicyTest {
   @Test
   void capsGenerationAtVanillaDepthWhenTheTerrainShellIsDeeper() {
      assertEquals(64, UndergroundGenerationDepthPolicy.generationDepth(64));
      assertEquals(64, UndergroundGenerationDepthPolicy.generationDepth(192));
      assertEquals(936, UndergroundGenerationDepthPolicy.generationFloorY(1_000, 512));
      assertEquals(937, UndergroundGenerationDepthPolicy.deepestGenerationY(1_000, 512));
   }

   @Test
   void treatsTheSixtyFourBlockBoundaryAsProtectedTerrain() {
      assertTrue(UndergroundGenerationDepthPolicy.containsDepth(63, 512));
      assertFalse(UndergroundGenerationDepthPolicy.containsDepth(64, 512));
      assertFalse(UndergroundGenerationDepthPolicy.containsDepth(400, 512));
   }

   @Test
   void stillHonorsAConfigurableShellIfItIsEverShallowerThanVanilla() {
      assertEquals(20, UndergroundGenerationDepthPolicy.generationDepth(20));
      assertTrue(UndergroundGenerationDepthPolicy.containsDepth(19, 20));
      assertFalse(UndergroundGenerationDepthPolicy.containsDepth(20, 20));
   }

   @Test
   void caveAndOreExtentFillsTheWholeShellInsteadOfTheVanillaBand() {
      // Unlike the vanilla biome band, caves and ores follow the full configured
      // shell thickness rather than being capped at 64 blocks.
      assertEquals(256, UndergroundGenerationDepthPolicy.caveOreDepth(256));
      // A giant mountain (surface 250, default 256 shell, deep world floor) is
      // carved down to its support bottom, not to surface - 64.
      assertEquals(-6, UndergroundGenerationDepthPolicy.caveOreFloorY(250, 256, -368));
      assertEquals(-5, UndergroundGenerationDepthPolicy.deepestCaveOreY(250, 256, -368));
   }

   @Test
   void caveAndOreExtentAdaptsToTheWorldMinimumBuildHeight() {
      // A player-lowered world floor (e.g. -260) is honored dynamically instead
      // of stopping at the old hard-coded shallow limit.
      assertEquals(-260, UndergroundGenerationDepthPolicy.caveOreFloorY(64, 512, -260));
      // Never carve below the world floor even when the shell would reach lower.
      assertEquals(-64, UndergroundGenerationDepthPolicy.caveOreFloorY(64, 256, -64));
   }

   @Test
   void caveAndOreDepthIsCappedForLoopSafety() {
      assertEquals(UndergroundGenerationDepthPolicy.MAX_CAVE_ORE_DEPTH, UndergroundGenerationDepthPolicy.caveOreDepth(9_999));
      assertEquals(0, UndergroundGenerationDepthPolicy.caveOreDepth(-5));
   }
}
