package com.yucareux.tellus.mixin.client;

import com.yucareux.tellus.client.screen.EarthCustomizeScreen;
import com.yucareux.tellus.worldgen.TellusWorldPresets;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldCreationUiState.class)
public abstract class PresetEditorMixin {
   @Redirect(
      method = "getPresetEditor",
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;",
         remap = false
      )
   )
   private Object tellus$addEarthEditor(Map<Object, Object> editors, Object key) {
      Object existing = editors.get(key);
      if (existing != null) {
         return existing;
      }

      if (key.equals(Optional.of(TellusWorldPresets.EARTH))) {
         PresetEditor earthEditor = EarthCustomizeScreen::new;
         return earthEditor;
      }

      return null;
   }
}
