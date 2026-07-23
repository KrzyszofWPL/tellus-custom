package com.yucareux.tellus.network;

import net.minecraft.network.FriendlyByteBuf;

public record GeoTpTeleportPayload(double latitude, double longitude) {
   public GeoTpTeleportPayload(FriendlyByteBuf buffer) {
      this(buffer.readDouble(), buffer.readDouble());
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.writeDouble(this.latitude());
      buffer.writeDouble(this.longitude());
   }
}
