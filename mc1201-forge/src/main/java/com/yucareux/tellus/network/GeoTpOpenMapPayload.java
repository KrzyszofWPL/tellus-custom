package com.yucareux.tellus.network;

import net.minecraft.network.FriendlyByteBuf;

public record GeoTpOpenMapPayload(double latitude, double longitude) {
   public GeoTpOpenMapPayload(FriendlyByteBuf buffer) {
      this(buffer.readDouble(), buffer.readDouble());
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.writeDouble(this.latitude());
      buffer.writeDouble(this.longitude());
   }
}
