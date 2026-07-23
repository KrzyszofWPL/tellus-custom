package com.yucareux.tellus.worldgen.caves;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

/**
 * Named replacement for the anonymous {@link WorldGenerationContext} subclass used by
 * {@code HeightRangePlacementMixin}. Forge ships upstream SpongePowered Mixin, whose
 * annotation processor cannot handle anonymous classes declared inside a mixin under
 * searge obfuscation, so the subclass is extracted into a regular (non-mixin) class.
 */
public final class TellusVanillaWorldGenerationContext extends WorldGenerationContext {
   public TellusVanillaWorldGenerationContext(ChunkGenerator generator, LevelHeightAccessor heightAccessor) {
      super(generator, heightAccessor);
   }

   @Override
   public int getMinGenY() {
      return TellusCaveDepthMapper.VANILLA_MIN_Y;
   }

   @Override
   public int getGenDepth() {
      return TellusCaveDepthMapper.VANILLA_MAX_Y - TellusCaveDepthMapper.VANILLA_MIN_Y + 1;
   }
}
