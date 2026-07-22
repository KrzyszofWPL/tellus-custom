package com.yucareux.tellus;

import com.yucareux.tellus.client.hud.ManagedTerrainDownloadOverlay;
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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class TellusClient {
   private static final KeyMapping OPEN_MAP_KEY = new KeyMapping(
      "key.tellus.open_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.category.tellus.controls"
   );
   private static int managedTerrainViewUpdateTicks;

   private TellusClient() {
   }

   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(TellusClient::registerGuiOverlays);
      modEventBus.addListener(TellusClient::registerKeyMappings);
      MinecraftForge.EVENT_BUS.addListener(TellusClient::onClientDisconnect);
      MinecraftForge.EVENT_BUS.addListener(TellusClient::onClientTick);
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
