package com.yucareux.tellus.platform;

import java.nio.file.Path;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

public final class ForgeTellusPlatformService implements TellusPlatformService {
   @Override
   public Path gameDir() {
      return FMLPaths.GAMEDIR.get();
   }

   @Override
   public Path configDir() {
      return FMLPaths.CONFIGDIR.get();
   }

   @Override
   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }
}
