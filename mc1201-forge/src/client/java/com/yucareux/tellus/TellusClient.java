package com.yucareux.tellus;

import com.yucareux.tellus.client.hud.ManagedTerrainDownloadOverlay;
import com.yucareux.tellus.client.screen.EarthCustomizeScreen;
import com.yucareux.tellus.client.screen.EarthTeleportScreen;
import com.yucareux.tellus.integration.distant_horizons.managed.ManagedTerrainClientState;
import com.yucareux.tellus.integration.distant_horizons.managed.ManagedTerrainViewDistance;
import com.yucareux.tellus.network.GeoTpOpenMapPayload;
import com.yucareux.tellus.network.ManagedTerrainStatusPayload;
import com.yucareux.tellus.network.ManagedTerrainViewPayload;
import com.yucareux.tellus.network.TellusForgeClientNetworking;
import com.yucareux.tellus.network.TellusWeatherPayload;
import com.yucareux.tellus.world.realtime.SnowGrid;
import com.yucareux.tellus.world.realtime.TemperatureGrid;
import com.yucareux.tellus.world.realtime.TellusRealtimeState;
import com.yucareux.tellus.worldgen.TellusWorldPresets;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TellusClient {
   private static final KeyMapping OPEN_MAP_KEY = new KeyMapping(
      "key.tellus.open_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.category.tellus.controls"
   );
   private static int managedTerrainViewUpdateTicks;

   private TellusClient() {
   }

   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(TellusClient::onClientSetup);
      modEventBus.addListener(TellusClient::registerGuiOverlays);
      modEventBus.addListener(TellusClient::registerKeyMappings);
      MinecraftForge.EVENT_BUS.addListener(TellusClient::onClientDisconnect);
      MinecraftForge.EVENT_BUS.addListener(TellusClient::onClientTick);
   }

   /**
    * Registers the Earth world preset editor by modifying {@link PresetEditor#EDITORS} at client
    * setup time. We cannot use a Mixin here because {@link PresetEditor} is a Java interface and
    * Mixin 0.8.5 does not support injectors on interface mixins. Instead we use
    * {@code sun.misc.Unsafe} to overwrite the {@code static final} field value, which is the
    * standard pattern for this kind of problem in the Forge ecosystem.
    */
   @SuppressWarnings("unchecked")
   private static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);

            Field editorsField = PresetEditor.class.getDeclaredField("EDITORS");
            long offset = unsafe.staticFieldOffset(editorsField);
            Object base = unsafe.staticFieldBase(editorsField);

            Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> oldEditors =
               (Map<Optional<ResourceKey<WorldPreset>>, PresetEditor>) unsafe.getObject(base, offset);

            Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> newEditors = new HashMap<>(oldEditors);
            newEditors.put(Optional.of(TellusWorldPresets.EARTH), EarthCustomizeScreen::new);
            unsafe.putObject(base, offset, Collections.unmodifiableMap(newEditors));

            Tellus.LOGGER.info("[Tellus] Registered Earth world preset editor.");
         } catch (Exception e) {
            Tellus.LOGGER.error("[Tellus] Failed to register Earth world preset editor.", e);
         }
      });
   }

   private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(OPEN_MAP_KEY);
   }

   private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAboveAll(
         "managed_terrain_status", (gui, graphics, partialTick, screenWidth, screenHeight) -> ManagedTerrainDownloadOverlay.render(graphics)
      );
   }

   public static void handleOpenMapPayload(GeoTpOpenMapPayload payload) {
      Minecraft minecraft = Minecraft.getInstance();
      Screen parent = minecraft.screen;
      minecraft.setScreen(new EarthTeleportScreen(parent, payload.latitude(), payload.longitude()));
   }

   public static void handleWeatherPayload(TellusWeatherPayload payload) {
      SnowGrid grid = payload.historicalSnowEnabled() && payload.spacingBlocks() > 0
         ? new SnowGrid(payload.centerX(), payload.centerZ(), payload.spacingBlocks(), payload.snowIndex())
         : SnowGrid.empty();
      TemperatureGrid temperatureGrid = payload.spacingBlocks() > 0
         ? new TemperatureGrid(
            payload.centerX(),
            payload.centerZ(),
            payload.spacingBlocks(),
            payload.temperatureC(),
            System.currentTimeMillis() - Math.max(0L, payload.temperatureAgeMs())
         )
         : TemperatureGrid.empty();
      TellusRealtimeState.updateWeatherState(
         payload.weatherEnabled(), payload.precipitationMode(), payload.historicalSnowEnabled(), grid, temperatureGrid
      );
   }

   public static void handleManagedTerrainStatusPayload(ManagedTerrainStatusPayload payload) {
      ManagedTerrainClientState.update(payload.status());
   }

   private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
      TellusRealtimeState.reset();
      ManagedTerrainClientState.reset();
      managedTerrainViewUpdateTicks = 0;
   }

   private static void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.END) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      while (OPEN_MAP_KEY.consumeClick()) {
         if (minecraft.screen == null && minecraft.player != null && minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand("tellus map");
         }
      }

      if (minecraft.player != null && ++managedTerrainViewUpdateTicks >= 40) {
         managedTerrainViewUpdateTicks = 0;
         TellusForgeClientNetworking.sendToServer(new ManagedTerrainViewPayload(ManagedTerrainViewDistance.detect()));
      }
   }
}
