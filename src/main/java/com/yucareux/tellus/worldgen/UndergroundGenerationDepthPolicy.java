package com.yucareux.tellus.worldgen;

/**
 * Defines the surface-relative bands used by Tellus underground content.
 *
 * <p>Two distinct extents are described here:</p>
 * <ul>
 *   <li>The <b>vanilla biome band</b> ({@link #generationDepth}) keeps cave
 *       biomes, deep-dark placement and underground structure probes inside a
 *       stable, vanilla-sized window below the surface. Increasing the terrain
 *       shell thickness must not stretch those systems into deeper rock.</li>
 *   <li>The <b>cave &amp; ore extent</b> ({@link #caveOreFloorY}) fills the whole
 *       solid terrain shell. Caves and ores follow the shell all the way down to
 *       its support bottom, which itself adapts to the configured underground
 *       depth and, crucially, to the world's own minimum build height. This is
 *       what lets tall mountains carve caves through their entire mass and deep
 *       custom worlds (for example a -260 or -368 floor) generate caves and ores
 *       far below the old fixed limit instead of stopping near the surface.</li>
 * </ul>
 */
public final class UndergroundGenerationDepthPolicy {
   public static final int MAX_DEPTH_BELOW_SURFACE = 64;

   /**
    * Hard safety ceiling for the adaptive cave/ore depth so a misconfigured
    * shell can never spin the carving loops for an unbounded number of blocks.
    * Matches the maximum configurable terrain-shell thickness.
    */
   public static final int MAX_CAVE_ORE_DEPTH = 512;

   private UndergroundGenerationDepthPolicy() {
   }

   public static int generationDepth(int undergroundDepth) {
      return Math.min(Math.max(undergroundDepth, 0), MAX_DEPTH_BELOW_SURFACE);
   }

   /**
    * Returns the protected floor below vanilla-band underground content. Content
    * may be placed above this Y, but never at or below it.
    */
   public static int generationFloorY(int surfaceY, int undergroundDepth) {
      return surfaceY - generationDepth(undergroundDepth);
   }

   public static int deepestGenerationY(int surfaceY, int undergroundDepth) {
      return generationFloorY(surfaceY, undergroundDepth) + 1;
   }

   public static boolean containsDepth(int depthBelowSurface, int undergroundDepth) {
      return depthBelowSurface >= 0 && depthBelowSurface < generationDepth(undergroundDepth);
   }

   /**
    * Depth (in blocks below the surface) that caves and ores are allowed to
    * reach. Unlike {@link #generationDepth} this tracks the full configured
    * terrain-shell thickness rather than the fixed vanilla band, capped only by
    * {@link #MAX_CAVE_ORE_DEPTH} for loop safety.
    */
   public static int caveOreDepth(int undergroundDepth) {
      return Math.min(Math.max(undergroundDepth, 0), MAX_CAVE_ORE_DEPTH);
   }

   /**
    * Lowest solid Y that caves and ores follow in a column: the terrain shell's
    * support bottom. The result is clamped to {@code worldMinY} so the extent
    * adapts to whatever minimum build height the dimension (or the player's
    * world settings) actually uses, replacing the previous hard-coded band.
    */
   public static int caveOreFloorY(int surfaceY, int undergroundDepth, int worldMinY) {
      long floorY = (long) surfaceY - caveOreDepth(undergroundDepth);
      return (int) Math.max(worldMinY, floorY);
   }

   /**
    * Deepest Y at which caves and ores may generate (one block above the
    * bedrock support bottom returned by {@link #caveOreFloorY}).
    */
   public static int deepestCaveOreY(int surfaceY, int undergroundDepth, int worldMinY) {
      return caveOreFloorY(surfaceY, undergroundDepth, worldMinY) + 1;
   }
}
