package com.yucareux.tellus.network;

import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.TellusClient;
import java.util.Optional;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TellusForgeNetworking {
   private static final String PROTOCOL_VERSION = "1";
   private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation(Tellus.MOD_ID, "main"),
      () -> PROTOCOL_VERSION,
      PROTOCOL_VERSION::equals,
      PROTOCOL_VERSION::equals
   );

   private TellusForgeNetworking() {
   }

   public static void registerPackets() {
      int id = 0;

      CHANNEL.registerMessage(
         id++,
         GeoTpTeleportPayload.class,
         GeoTpTeleportPayload::write,
         GeoTpTeleportPayload::new,
         (payload, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ServerPlayer player = ctx.getSender();
            ctx.enqueueWork(() -> {
               if (player != null) {
                  Tellus.handleGeoTeleport(payload, player);
               }
            });
            ctx.setPacketHandled(true);
         },
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );

      CHANNEL.registerMessage(
         id++,
         ManagedTerrainViewPayload.class,
         ManagedTerrainViewPayload::write,
         ManagedTerrainViewPayload::new,
         (payload, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ServerPlayer player = ctx.getSender();
            ctx.enqueueWork(() -> {
               if (player != null) {
                  Tellus.handleManagedTerrainView(payload, player);
               }
            });
            ctx.setPacketHandled(true);
         },
         Optional.of(NetworkDirection.PLAY_TO_SERVER)
      );

      CHANNEL.registerMessage(
         id++,
         GeoTpOpenMapPayload.class,
         GeoTpOpenMapPayload::write,
         GeoTpOpenMapPayload::new,
         (payload, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
               if (FMLEnvironment.dist == Dist.CLIENT) {
                  TellusClient.handleOpenMapPayload(payload);
               }
            });
            ctx.setPacketHandled(true);
         },
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );

      CHANNEL.registerMessage(
         id++,
         TellusWeatherPayload.class,
         TellusWeatherPayload::write,
         TellusWeatherPayload::new,
         (payload, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
               if (FMLEnvironment.dist == Dist.CLIENT) {
                  TellusClient.handleWeatherPayload(payload);
               }
            });
            ctx.setPacketHandled(true);
         },
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );

      CHANNEL.registerMessage(
         id++,
         ManagedTerrainStatusPayload.class,
         ManagedTerrainStatusPayload::write,
         ManagedTerrainStatusPayload::new,
         (payload, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
               if (FMLEnvironment.dist == Dist.CLIENT) {
                  TellusClient.handleManagedTerrainStatusPayload(payload);
               }
            });
            ctx.setPacketHandled(true);
         },
         Optional.of(NetworkDirection.PLAY_TO_CLIENT)
      );
   }

   public static void sendToPlayer(ServerPlayer player, Object payload) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
   }

   public static void sendToServer(Object payload) {
      CHANNEL.sendToServer(payload);
   }

   public static boolean isRemotePresent(Connection connection) {
      return CHANNEL.isRemotePresent(connection);
   }
}
