package com.yucareux.tellus.mixin.client;

import com.yucareux.tellus.client.LoadingTerrainScreenTiming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenMixin {
   // NOTE: this build targets Forge 1.20.1 through the legacyForge (SRG runtime)
   // toolchain. A `@Shadow public Screen screen;` field could not be remapped
   // from the Mojmap name to its SRG name at apply time, which crashed the game
   // on startup ("@Shadow field screen was not located in the target class").
   // Instead of shadowing the field we read the current screen straight from the
   // Minecraft singleton inside the injected body. Ordinary field references in a
   // mixin body are reobfuscated by the build, exactly like the sibling
   // LevelLoadingScreenMixin already reads Minecraft.getInstance().font, so this
   // resolves correctly on Forge/SRG, Fabric/intermediary and NeoForge/Mojmap.
   @Inject(
      method = {"setScreen", "method_1507", "m_91152_"},
      at = @At("HEAD"),
      remap = false
   )
   private void tellus$trackLoadingTerrainScreen(Screen newScreen, CallbackInfo ci) {
      // At HEAD of setScreen the field still holds the outgoing screen.
      Screen previousScreen = Minecraft.getInstance().screen;
      LoadingTerrainScreenTiming.onScreenChange(previousScreen, newScreen);
   }
}
